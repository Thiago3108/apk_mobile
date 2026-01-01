package com.thiago.apk_mobile.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.thiago.apk_mobile.data.InventarioDatabase
import com.thiago.apk_mobile.util.DriveHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val googleUser: GoogleSignInAccount? = null,
    val driveFiles: List<com.google.api.services.drive.model.File> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val db: InventarioDatabase
) : ViewModel() {

    private val dbName = "inventario_db"
    private val appFolderName = "ApkMobileBackup"

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private lateinit var googleSignInClient: GoogleSignInClient
    private var driveHelper: DriveHelper? = null

    init {
        setupGoogleSignIn()
        checkForExistingSignIn()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestIdToken("379430061796-tqnl97p1oga8s409hd73tfemnbhrels0.apps.googleusercontent.com")
            .build()
        googleSignInClient = GoogleSignIn.getClient(app, gso)
    }

    private fun checkForExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(app)
        if (account != null && account.grantedScopes.contains(Scope(DriveScopes.DRIVE_FILE))) {
            onGoogleSignInSuccess(account)
        } else {
            _uiState.update { it.copy(isLoading = false, googleUser = null) }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            val signInIntent = googleSignInClient.signInIntent
            _eventFlow.emit(UiEvent.LaunchGoogleSignIn(signInIntent))
        }
    }

    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener { 
            _uiState.update { it.copy(isLoading = false, googleUser = null, driveFiles = emptyList()) }
            driveHelper = null
        }
    }

    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        _uiState.update { it.copy(isLoading = false, googleUser = account) }
        driveHelper = DriveHelper(app, account)
        loadBackupFiles()
    }

    fun onGoogleSignInFailed() {
        _uiState.update { it.copy(isLoading = false, googleUser = null) }
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Error al iniciar sesión con Google."))
        }
    }
    
    private suspend fun getAppFolderId(drive: Drive): String? = withContext(Dispatchers.IO) {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$appFolderName' and trashed=false"
        val result = drive.files().list().setQ(query).setSpaces("drive").setFields("files(id)").execute()
        result.files.firstOrNull()?.id
    }

    private suspend fun createAppFolder(drive: Drive): String = withContext(Dispatchers.IO) {
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = appFolderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val file = drive.files().create(folderMetadata).setFields("id").execute()
        file.id
    }
    

    fun exportDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) {
                    val drive = driveHelper?.drive ?: throw IllegalStateException("Drive not initialized")
                    val folderId = getAppFolderId(drive) ?: createAppFolder(drive)

                    db.close()
                    val dbFile = app.getDatabasePath(dbName)
                    val backupFile = File(app.cacheDir, "backup.zip")
                    if (backupFile.exists()) backupFile.delete()

                    ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                        listOf(dbFile, File(dbFile.parent, "$dbName-shm"), File(dbFile.parent, "$dbName-wal")).filter { it.exists() }.forEach { file ->
                            zos.putNextEntry(ZipEntry(file.name))
                            FileInputStream(file).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }

                    val fileMetadata = com.google.api.services.drive.model.File().apply {
                        name = "backup_${System.currentTimeMillis()}.zip"
                        parents = listOf(folderId)
                    }
                    val mediaContent = FileContent("application/zip", backupFile)
                    drive.files().create(fileMetadata, mediaContent).execute()
                    
                    backupFile.delete()
                    loadBackupFiles()
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Exportación a Drive completada."))
                _eventFlow.emit(UiEvent.RestartApp)
            } catch (e: Exception) {
                e.printStackTrace()
                _eventFlow.emit(UiEvent.ShowSnackbar("Error al exportar a Drive: ${e.message}"))
                 _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun importDatabase(fileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) {
                    val drive = driveHelper?.drive ?: throw IllegalStateException("Drive not initialized")
                    val dbFolder = app.getDatabasePath(dbName).parentFile

                    db.close()
                    dbFolder?.listFiles()?.filter { it.name.startsWith(dbName) }?.forEach { it.delete() }
                    
                    val outputStream = FileOutputStream(File(dbFolder, "backup.zip"))
                    drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

                    ZipInputStream(FileInputStream(File(dbFolder, "backup.zip"))).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val newFile = File(dbFolder, entry.name)
                            FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                            entry = zis.nextEntry
                        }
                        zis.closeEntry()
                    }
                    File(dbFolder, "backup.zip").delete()
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Importación completada. Reiniciando..."))
                _eventFlow.emit(UiEvent.RestartApp)
            } catch (e: Exception) {
                e.printStackTrace()
                _eventFlow.emit(UiEvent.ShowSnackbar("Error al importar: ${e.message}"))
                 _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadBackupFiles(){
        viewModelScope.launch{
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO){
                    val drive = driveHelper?.drive ?: return@withContext
                    val folderId = getAppFolderId(drive) ?: return@withContext
                    val query = "'$folderId' in parents and mimeType='application/zip' and trashed=false"
                    val result = drive.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, name, createdTime)")
                        .setOrderBy("createdTime desc")
                        .execute()
                    _uiState.update{ it.copy(driveFiles = result.files ?: emptyList()) }
                }
            } catch(e: Exception){
                e.printStackTrace()
                _eventFlow.emit(UiEvent.ShowSnackbar("No se pudieron cargar las copias de seguridad: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object RestartApp : UiEvent()
        data class LaunchGoogleSignIn(val signInIntent: Intent) : UiEvent()
    }
}
