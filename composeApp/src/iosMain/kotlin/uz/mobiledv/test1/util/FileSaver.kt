@file:OptIn(ExperimentalForeignApi::class)

package uz.mobiledv.test1.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.UIDocumentInteractionController

actual class FileSaver {

    actual suspend fun saveFile(fileData: FileData): String? = withContext(Dispatchers.Main) {
        try {
            val documentsPath = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).firstOrNull() as? String ?: return@withContext null

            val filePath = "$documentsPath/${fileData.name}"
            val nsData = fileData.bytes.toNSData()

            val success = nsData.writeToFile(filePath, atomically = true)
            if (success) filePath else null
        } catch (e: Exception) {
            println("Error saving file: ${e.message}")
            null
        }
    }

    actual suspend fun saveFileToPublicDownloads(fileData: FileData): String? = withContext(Dispatchers.Main) {
        // iOS doesn't have a public downloads folder like Android
        // Save to Documents directory instead
        saveFile(fileData)
    }
}

private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}