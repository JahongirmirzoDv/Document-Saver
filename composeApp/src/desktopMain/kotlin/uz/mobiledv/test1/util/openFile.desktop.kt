package uz.mobiledv.test1.util

import java.awt.Desktop
import java.io.File

actual fun openFile(filePath: String, mimeType: String?) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            } else {
                println("Desktop operations not supported on this system.")
            }
        } else {
            println("Error: File not found at path: $filePath")
        }
    } catch (e: Exception) {
        println("Error opening file on desktop: ${e.message}")
        e.printStackTrace()
    }
}

actual fun openFileLocationInFileManager() {
}