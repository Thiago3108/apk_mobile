package com.thiago.apk_mobile.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.thiago.apk_mobile.data.InventarioDatabase
import com.thiago.apk_mobile.util.DriveHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: InventarioDatabase
) : CoroutineWorker(context, workerParams) {

    private val dbName = "inventario_db"
    private val appFolderName = "ApkMobileBackup"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null || !account.grantedScopes.contains(Scope(DriveScopes.DRIVE_FILE))) {
                // Si no hay usuario o no hay permisos, no se puede hacer la copia.
                return@withContext Result.failure()
            }

            val driveHelper = DriveHelper(context, account)
            val drive = driveHelper.drive
            val folderId = getAppFolderId(drive) ?: createAppFolder(drive)

            db.close()
            val dbFile = context.getDatabasePath(dbName)
            val backupFile = File(context.cacheDir, "backup_auto.zip")
            if (backupFile.exists()) backupFile.delete()

            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                listOf(dbFile, File(dbFile.parent, "$dbName-shm"), File(dbFile.parent, "$dbName-wal")).filter { it.exists() }.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    FileInputStream(file).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "backup_auto_${System.currentTimeMillis()}.zip"
                parents = listOf(folderId)
            }
            val mediaContent = FileContent("application/zip", backupFile)
            drive.files().create(fileMetadata, mediaContent).execute()
            
            backupFile.delete()

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun getAppFolderId(drive: Drive): String? {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$appFolderName' and trashed=false"
        val result = drive.files().list().setQ(query).setSpaces("drive").execute()
        return result.files.firstOrNull()?.id
    }

    private fun createAppFolder(drive: Drive): String {
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = appFolderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val file = drive.files().create(folderMetadata).setFields("id").execute()
        return file.id
    }
}
