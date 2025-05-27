package uz.mobiledv.test1.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual class FileSaver(private val context: Context) {

    actual suspend fun saveFile(fileData: FileData): String? = withContext(Dispatchers.IO) {
        try {
            // Create a dedicated subdirectory in the cache for shared files
            val sharedDir = File(context.cacheDir, "shared_files")
            if (!sharedDir.exists()) {
                sharedDir.mkdirs()
            }

            // Ensure unique file names or handle overwriting if necessary
            // For simplicity, this example overwrites if a file with the same name exists.
            // You might want to generate unique names (e.g., using UUID) if overwriting is not desired.
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
}