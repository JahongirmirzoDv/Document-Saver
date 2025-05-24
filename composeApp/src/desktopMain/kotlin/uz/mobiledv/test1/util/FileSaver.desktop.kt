package uz.mobiledv.test1.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual class FileSaver {
    actual suspend fun saveFile(fileData: FileData): Boolean = withContext(Dispatchers.IO) {
        var success = false
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
                    success = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    success = false
                }
            } else {
                success = false // User cancelled
            }
        }
        success
    }
}