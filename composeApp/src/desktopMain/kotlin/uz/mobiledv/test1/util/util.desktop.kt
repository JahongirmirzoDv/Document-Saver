package uz.mobiledv.test1.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

actual class FilePicker { // No constructor parameter needed for desktop usually

    actual suspend fun pickFile(allowedTypes: List<String>): FileData? = suspendCoroutine { continuation ->
        SwingUtilities.invokeLater { // Ensure UI operations are on the EDT
            val fileChooser = JFileChooser()
            if (allowedTypes.isNotEmpty() && !(allowedTypes.size == 1 && allowedTypes[0] == "*/*")) {
                val filters = allowedTypes.map { type ->
                    // Basic mapping, might need more sophisticated MIME to extension logic
                    val extension = type.substringAfterLast('/', type).substringAfterLast('.', type)
                    val description = type.substringBeforeLast('/', type).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " (*.$extension)"
                    FileNameExtensionFilter(description, extension.lowercase(), extension.uppercase())
                }.toTypedArray()
                fileChooser.isAcceptAllFileFilterUsed = false // Don't show "All Files" if specific types given
                filters.forEach { fileChooser.addChoosableFileFilter(it) }
                if (filters.isNotEmpty()) {
                    fileChooser.fileFilter = filters[0] // Set the first one as default
                }
            } else {
                fileChooser.isAcceptAllFileFilterUsed = true
            }


            // Find an active window to parent the dialog, or null
            val parentWindow = Frame.getFrames().firstOrNull { it.isActive }

            val result = fileChooser.showOpenDialog(parentWindow)

            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                try {
                    val bytes = file.readBytes()
                    // MIME type detection on desktop is not as straightforward as on Android.
                    // You might use java.nio.file.Files.probeContentType(file.toPath())
                    // or a library like Apache Tika if precise MIME types are critical.
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
    val filePicker = remember { FilePicker() } // Create an instance of the desktop FilePicker
    // allowedTypes can be passed to the launcher if needed, e.g., from a button click
    return {
        // This example launches with no specific file type filter.
        // You can enhance this to accept allowedTypes similar to the Android version.
        // For simplicity, this directly calls the suspend function.
        // A more complex version might use a coroutine scope tied to the Composable.
        // This is a simplified way to bridge it for desktop.
        // Ideally, you'd manage the coroutine scope carefully.
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { // Not ideal to use GlobalScope, use a proper scope
            val fileData = filePicker.pickFile(listOf("*/*")) // Example: pick any file
            // Switch back to main thread if onFilePicked interacts with Compose state directly
            // For now, assuming onFilePicked handles its own threading if necessary
            onFilePicked(fileData)
        }
    }
}