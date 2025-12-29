package com.thiago.apk_mobile.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.apk_mobile.data.InventarioDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val db: InventarioDatabase
) : ViewModel() {

    private val dbName = "inventario_db"

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            try {
                db.close()

                val dbFile = app.getDatabasePath(dbName)
                val dbShm = File(dbFile.parent, "$dbName-shm")
                val dbWal = File(dbFile.parent, "$dbName-wal")

                val filesToZip = listOf(dbFile, dbShm, dbWal).filter { it.exists() }

                if (filesToZip.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Error: No se encontró la base de datos."))
                    return@launch
                }

                app.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        filesToZip.forEach { file ->
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Exportación completada con éxito."))
                _eventFlow.emit(UiEvent.RestartApp)

            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error al exportar: ${e.message}"))
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            try {
                db.close()

                val dbFolder = app.getDatabasePath(dbName).parentFile
                dbFolder?.listFiles()?.filter { it.name.startsWith(dbName) }?.forEach { it.delete() }

                app.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var zipEntry = zis.nextEntry
                        while (zipEntry != null) {
                            val newFile = File(dbFolder, zipEntry.name)
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            zipEntry = zis.nextEntry
                        }
                        zis.closeEntry()
                    }
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Importación completada. Reiniciando..."))
                _eventFlow.emit(UiEvent.RestartApp)

            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error al importar: ${e.message}"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object RestartApp : UiEvent()
    }
}
