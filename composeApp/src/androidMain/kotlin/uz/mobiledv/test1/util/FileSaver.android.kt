package uz.mobiledv.test1.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

actual class FileSaver(private val context: Context) {

    actual suspend fun saveFile(fileData: FileData): String? = withContext(Dispatchers.IO) {
        try {
            // Create a dedicated subdirectory in the cache for shared files
            val sharedDir = File(context.cacheDir, "shared_files")
            if (!sharedDir.exists()) {
                sharedDir.mkdirs()
            }

            val outputFile = File(sharedDir, fileData.name)

            FileOutputStream(outputFile).use { fos ->
                fos.write(fileData.bytes)
            }
            outputFile.absolutePath // Return the absolute file path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun saveFileToPublicDownloads(fileData: FileData): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = fileData.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android Q (API 29) and above for Scoped Storage compatibility
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, fileData.mimeType ?: "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(fileData.bytes)
                    }
                    return@withContext uri.toString() // Return URI path
                }
                return@withContext null
            } else {
                // For older versions (API < 29), direct file path (requires WRITE_EXTERNAL_STORAGE permission)
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val outputFile = File(downloadsDir, fileName)
                FileOutputStream(outputFile).use { fos ->
                    fos.write(fileData.bytes)
                }
                return@withContext outputFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}