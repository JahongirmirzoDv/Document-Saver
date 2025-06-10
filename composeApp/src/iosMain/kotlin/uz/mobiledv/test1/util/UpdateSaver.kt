package uz.mobiledv.test1.util

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class UpdateSaver {
    actual suspend fun saveFile(fileName: String, targetDirectory: String, writer: suspend (sink: Sink) -> Unit): String? {
        return try {
            val cacheDir = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
            val baseDirPath = (cacheDir + "/" + targetDirectory).toPath()
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