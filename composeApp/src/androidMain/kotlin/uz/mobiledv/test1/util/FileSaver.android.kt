package uz.mobiledv.test1.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.mobiledv.test1.MyActivity // To get application context

actual class FileSaver(private val context: Context) { // Pass context

    actual suspend fun saveFile(fileData: FileData): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileData.name)
                fileData.mimeType?.let { put(MediaStore.MediaColumns.MIME_TYPE, it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                // For older versions, you might need to handle file paths directly and request permission.
                // This is a simplified placeholder for brevity.
                // Consider using legacy storage for < Q if strictly necessary.
                // For now, we'll assume Q+ for cleaner MediaStore usage.
                // If targeting older versions, this part needs more robust handling.
                resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            }

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(fileData.bytes)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}