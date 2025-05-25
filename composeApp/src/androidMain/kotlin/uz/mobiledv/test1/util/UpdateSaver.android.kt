package uz.mobiledv.test1.util

import android.content.Context
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import uz.mobiledv.test1.MyActivity // To get application context

actual class UpdateSaver(private val context: Context) { // Ensure context is passed via DI
    actual suspend fun saveFile(fileName: String, targetDirectory: String, writer: suspend (sink: Sink) -> Unit): String? {
        return try {
            // Use app's cache directory for updates
            val baseDirPath = context.cacheDir.absolutePath.toPath() / targetDirectory
            if (!FileSystem.SYSTEM.exists(baseDirPath)) {
                FileSystem.SYSTEM.createDirectories(baseDirPath)
            }
            val outputFilePath = baseDirPath / fileName

            FileSystem.SYSTEM.sink(outputFilePath).use { sink ->
                writer(sink) // Write data using the provided lambda
            }
            outputFilePath.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
