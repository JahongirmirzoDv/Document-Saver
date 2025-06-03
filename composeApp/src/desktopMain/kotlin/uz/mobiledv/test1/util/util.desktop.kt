package uz.mobiledv.test1.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Frame
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class FilePicker {

    private fun configureFileChooser(fileChooser: JFileChooser, allowedTypes: List<String>) {
        if (allowedTypes.isNotEmpty() && !(allowedTypes.size == 1 && allowedTypes[0] == "*/*")) {
            val filters = allowedTypes.mapNotNull { type ->
                val parts = type.split("/")
                val generalType = parts.getOrNull(0)
                val specificType = parts.getOrNull(1)

                val extension: String = when (type) {
                    "application/pdf" -> "pdf"
                    "image/png" -> "png"
                    "image/jpeg" -> "jpeg"
                    "application/msword" -> "doc"
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                    "application/vnd.ms-excel" -> "xls"
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
                    else -> specificType?.substringAfterLast('.') ?: specificType ?: generalType ?: ""
                }
                if (extension.isNotBlank() && extension != "*") {
                    val description = "${specificType?.replaceFirstChar { it.titlecase() } ?: generalType?.replaceFirstChar { it.titlecase() } ?: "Files"} (*.$extension)"
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
                fileChooser.isAcceptAllFileFilterUsed = true // Keep "All Files" option
                filters.forEach { fileChooser.addChoosableFileFilter(it) }
                fileChooser.fileFilter = filters[0] // Set the first filter as default
            } else {
                fileChooser.isAcceptAllFileFilterUsed = true
            }
        } else {
            fileChooser.isAcceptAllFileFilterUsed = true
        }
    }

    private fun validateFile(file: java.io.File, allowedMimeTypesConfig: List<String>): FileData? {
        val probedMimeType = java.nio.file.Files.probeContentType(file.toPath()) ?: "application/octet-stream"

        val isMimeTypeAllowed = if (allowedMimeTypesConfig.contains("*/*") || allowedMimeTypesConfig.isEmpty()) {
            true
        } else {
            allowedMimeTypesConfig.contains(probedMimeType)
        }

        return if (isMimeTypeAllowed) {
            try {
                val bytes = file.readBytes()
                FileData(file.name, bytes, probedMimeType)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            println("Invalid file type selected on desktop: $probedMimeType (File: ${file.name}). Allowed: ${allowedMimeTypesConfig.joinToString()}")
            null
        }
    }

    actual suspend fun pickSingleFile(allowedTypes: List<String>): FileData? = suspendCoroutine { continuation ->
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser()
            fileChooser.isMultiSelectionEnabled = false
            configureFileChooser(fileChooser, allowedTypes)

            val parentWindow = Frame.getFrames().firstOrNull { it.isActive }
            val result = fileChooser.showOpenDialog(parentWindow)

            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                continuation.resume(validateFile(file, allowedTypes))
            } else {
                continuation.resume(null)
            }
        }
    }

    actual suspend fun pickMultipleFiles(allowedTypes: List<String>): List<FileData>? = suspendCoroutine { continuation ->
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser()
            fileChooser.isMultiSelectionEnabled = true
            configureFileChooser(fileChooser, allowedTypes)

            val parentWindow = Frame.getFrames().firstOrNull { it.isActive }
            val result = fileChooser.showOpenDialog(parentWindow)

            if (result == JFileChooser.APPROVE_OPTION) {
                val files = fileChooser.selectedFiles
                val fileDataList = files.mapNotNull { validateFile(it, allowedTypes) }
                continuation.resume(fileDataList.ifEmpty { null })
            } else {
                continuation.resume(null)
            }
        }
    }
}

@Composable
actual fun rememberSingleFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val filePicker = remember { FilePicker() }
    val scope = rememberCoroutineScope()

    return {
        scope.launch(Dispatchers.IO) { // Perform file picking on IO dispatcher
            val fileData = filePicker.pickSingleFile(allowedTypes)
            withContext(Dispatchers.Main) { // Switch back to Main for UI update
                onFilePicked(fileData)
            }
        }
    }
}

@Composable
actual fun rememberMultipleFilesPickerLauncher(
    onFilesPicked: (List<FileData>?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val filePicker = remember { FilePicker() }
    val scope = rememberCoroutineScope()

    return {
        scope.launch(Dispatchers.IO) { // Perform file picking on IO dispatcher
            val fileDataList = filePicker.pickMultipleFiles(allowedTypes)
            withContext(Dispatchers.Main) { // Switch back to Main for UI update
                onFilesPicked(fileDataList)
            }
        }
    }
}