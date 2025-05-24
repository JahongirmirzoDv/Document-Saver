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
                    val parts = type.split("/")
                    val generalType = parts.getOrNull(0)
                    val specificType = parts.getOrNull(1)

                    // More specific extension mapping
                    val extension: String = when (type) {
                        "application/pdf" -> "pdf"
                        "image/png" -> "png"
                        "image/jpeg" -> "jpeg"
                        "application/msword" -> "doc"
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                        "application/vnd.ms-excel" -> "xls"
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
                        else -> specificType?.substringAfterLast('.') ?: specificType ?: generalType ?: "data"
                    }
                    val description = "${specificType?.replaceFirstChar { it.titlecase() } ?: generalType?.replaceFirstChar { it.titlecase() } ?: "Files"} (*.$extension)"
                    if (extension != "*") {
                        // For JFileChooser, provide all variations of extensions if needed
                        val extensionsToFilter = when (type) {
                            "image/jpeg" -> arrayOf("jpeg", "jpg")
                            else -> arrayOf(extension.lowercase())
                        }
                        FileNameExtensionFilter(description, *extensionsToFilter)
                    } else {
                        null
                    }
                }.toTypedArray()

                if (filters.isNotEmpty()) {
                    fileChooser.isAcceptAllFileFilterUsed = true
                    filters.forEach { fileChooser.addChoosableFileFilter(it) }
                    fileChooser.fileFilter = filters[0]
                } else {
                    fileChooser.isAcceptAllFileFilterUsed = true
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
                    // Probe content type, then validate
                    val probedMimeType = java.nio.file.Files.probeContentType(file.toPath()) ?: "application/octet-stream"

                    val allowedMimeTypesForValidation = listOf(
                        "application/pdf", "image/png", "image/jpeg",
                        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )

                    if (allowedMimeTypesForValidation.contains(probedMimeType)) {
                        continuation.resume(FileData(file.name, bytes, probedMimeType))
                    } else {
                        println("Invalid file type selected on desktop: $probedMimeType (File: ${file.name})")
                        continuation.resume(null) // Invalid type
                    }
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
    val scope = rememberCoroutineScope()

    return {
        val allowedMimeTypes = listOf(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
        )
        scope.launch(Dispatchers.IO) {
            val fileData = filePicker.pickFile(allowedMimeTypes)
            withContext(Dispatchers.Main) {
                onFilePicked(fileData)
            }
        }
    }
}