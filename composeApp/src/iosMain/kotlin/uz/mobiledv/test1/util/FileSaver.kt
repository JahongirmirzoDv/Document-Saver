package uz.mobiledv.test1.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual class FileSaver {
    actual suspend fun saveFile(fileData: FileData): String? {
        val documentsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as NSString
        val filePath = documentsDir.stringByAppendingPathComponent(fileData.name)

        val nsData = fileData.bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = fileData.bytes.size.toULong())
        }

        return if (nsData.writeToFile(filePath, true)) {
            filePath
        } else {
            null
        }
    }

    actual suspend fun saveFileToPublicDownloads(fileData: FileData): String? {
       // On iOS, apps are sandboxed and cannot directly write to a public "Downloads" folder.
       // The common practice is to save to the app's own Documents directory
       // and optionally use UIDocumentInteractionController to let the user move it.
       return saveFile(fileData)
    }
}