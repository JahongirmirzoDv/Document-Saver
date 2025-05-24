package uz.mobiledv.test1.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope // Import rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class FilePicker {

    actual suspend fun pickFile(allowedTypes: List<String>): FileData? = suspendCoroutine { continuation ->
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser()
            if (allowedTypes.isNotEmpty() && !(allowedTypes.size == 1 && allowedTypes[0] == "*/*")) {
                val filters = allowedTypes.mapNotNull { type ->
                    // Improved MIME to extension mapping and description
                    val parts = type.split("/")
                    val generalType = parts.getOrNull(0)
                    val specificType = parts.getOrNull(1)

                    val extension = specificType?.substringAfterLast('.') ?: specificType ?: generalType ?: "data"
                    val description = "${specificType?.replaceFirstChar { it.titlecase() } ?: generalType?.replaceFirstChar { it.titlecase() } ?: "Files"} (*.$extension)"
                    if (extension != "*") {
                        FileNameExtensionFilter(description, extension.lowercase(), extension.uppercase())
                    } else {
                        null // Avoid creating a filter for "*/*" like this, handle separately or let JFileChooser do it
                    }
                }.toTypedArray()

                if (filters.isNotEmpty()) {
                    fileChooser.isAcceptAllFileFilterUsed = true // Still allow "All Files"
                    filters.forEach { fileChooser.addChoosableFileFilter(it) }
                    fileChooser.fileFilter = filters[0]
                } else {
                    fileChooser.isAcceptAllFileFilterUsed = true // Default to all files if no specific valid types
                }

            } else {
                fileChooser.isAcceptAllFileFilterUsed = true
            }

            val parentWindow = Frame.getFrames().firstOrNull { it.isActive }
            val result = fileChooser.showOpenDialog(parentWindow)

            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                try {
                    val bytes = file.readBytes()
                    val mimeType = java.nio.file.Files.probeContentType(file.toPath()) ?: "application/octet-stream"
                    continuation.resume(FileData(file.name, bytes, mimeType))
                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(null)
                }
            } else {
                continuation.resume(null)
            }
        }
    }
}

@Composable
actual fun rememberFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit
): () -> Unit {
    val filePicker = remember { FilePicker() }
    val scope = rememberCoroutineScope() // Use rememberCoroutineScope

    return {
        // Example: launch with a filter for images and PDFs, or all files if none specified
        val commonTypes = listOf("image/png", "image/jpeg", "application/pdf", "*/*")
        scope.launch(Dispatchers.IO) { // Perform file IO on a background thread
            val fileData = filePicker.pickFile(commonTypes) // Pass allowed types
            withContext(Dispatchers.Main) { // Switch back to main thread for UI updates
                onFilePicked(fileData)
            }
        }
    }
}