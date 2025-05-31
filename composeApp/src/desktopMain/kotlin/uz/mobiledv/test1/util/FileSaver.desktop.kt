package uz.mobiledv.test1.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual class FileSaver {
    actual suspend fun saveFile(fileData: FileData): String? = withContext(Dispatchers.IO) { // Changed return type
        var filePath: String? = null
        // Use SwingUtilities to ensure JFileChooser runs on the EDT
        SwingUtilities.invokeAndWait {
            val fileChooser = JFileChooser()
            fileChooser.selectedFile = File(fileData.name) // Suggest original name
            val result = fileChooser.showSaveDialog(null) // Pass a parent frame if available

            if (result == JFileChooser.APPROVE_OPTION) {
                val chosenFile = fileChooser.selectedFile
                try {
                    FileOutputStream(chosenFile).use { fos ->
                        fos.write(fileData.bytes)
                    }
                    filePath = chosenFile.absolutePath // Get the absolute path
                } catch (e: Exception) {
                    e.printStackTrace()
                    filePath = null
                }
            } else {
                filePath = null // User cancelled
            }
        }
        filePath
    }

    actual suspend fun saveFileToPublicDownloads(fileData: FileData): String? {
        // On desktop, the standard saveFile dialog allows the user to choose any location,
        // including their Downloads folder. So, we can reuse the existing logic.
        return saveFile(fileData)
    }
}