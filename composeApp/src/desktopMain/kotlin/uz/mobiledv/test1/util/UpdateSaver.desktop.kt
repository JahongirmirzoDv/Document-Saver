package uz.mobiledv.test1.util

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File // Still need java.io.File for System.getProperty

actual class UpdateSaver { // No context needed for desktop in this implementation
    actual suspend fun saveFile(fileName: String, targetDirectory: String, writer: suspend (sink: Sink) -> Unit): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val tempDir = System.getProperty("java.io.tmpdir")?.toPath()
                ?: File(".").absolutePath.toPath() // Fallback to current directory

            val baseDirPath = tempDir / targetDirectory.ifBlank { "app_updates_cache" }

            if (!FileSystem.SYSTEM.exists(baseDirPath)) {
                FileSystem.SYSTEM.createDirectories(baseDirPath)
            }
            val outputFilePath = baseDirPath / fileName

            FileSystem.SYSTEM.sink(outputFilePath).use { sink ->
                writer(sink)
            }
            outputFilePath.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
