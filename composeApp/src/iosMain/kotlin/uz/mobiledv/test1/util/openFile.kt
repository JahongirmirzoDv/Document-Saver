package uz.mobiledv.test1.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openFile(filePath: String, mimeType: String?) {
    val fileUrl = NSURL.fileURLWithPath(filePath)
    val application = UIApplication.sharedApplication
    if (application.canOpenURL(fileUrl)) {
        application.openURL(fileUrl)
    } else {
        println("Cannot open URL: $fileUrl")
    }
}

actual fun openFileLocationInFileManager() {
    // Direct access to a file manager like on Android is not possible.
    // You can open the app's documents directory if configured in Info.plist
    println("openFileLocationInFileManager is not directly supported on iOS.")
}