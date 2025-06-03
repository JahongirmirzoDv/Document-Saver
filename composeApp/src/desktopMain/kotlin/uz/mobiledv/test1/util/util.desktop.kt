package uz.mobiledv.test1.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog // Import FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter // Import FilenameFilter
import javax.swing.JFileChooser
import javax.swing.SwingUtilities // Keep for EDT operations if complex setup needed, though FileDialog is simpler

// Helper function to get the parent Frame, needed for FileDialog
private fun getParentFrame(): Frame? {
    // Attempt to find an active Frame. This might need to be more robust
    // depending on your application's window management.
    // If your app always has a main window that's a Frame, you can pass it directly.
    return Frame.getFrames().firstOrNull { it.isActive }
}

// Helper to convert MIME types to filename extensions for FilenameFilter
private fun createFilenameFilter(allowedTypes: List<String>): FilenameFilter? {
    if (allowedTypes.isEmpty() || allowedTypes.contains("*/*")) {
        return null // Accept all files
    }

    val extensions = allowedTypes.mapNotNull { type ->
        when (type) {
            "application/pdf" -> ".pdf"
            "image/png" -> ".png"
            "image/jpeg" -> ".jpeg" // Handle multiple common extensions
            "application/msword" -> ".doc"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
            "application/vnd.ms-excel" -> ".xls"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx"
            // Add more MIME type to extension mappings as needed
            else -> {
                val parts = type.split("/")
                parts.getOrNull(1)?.let { ".$it" } // Fallback to subtype as extension
            }
        }
    }.flatMap { ext -> if (ext.contains(",")) ext.split(",").map { it.trim() } else listOf(ext) }
        .map { it.lowercase() }
        .toSet()

    if (extensions.isEmpty()) return null

    return FilenameFilter { dir, name ->
        extensions.any { name.lowercase().endsWith(it) }
    }
}


actual class FilePicker {

    private fun processFileDialogResult(dialog: FileDialog, allowedMimeTypesConfig: List<String>): List<FileData>? {
        val files = dialog.files // For multiple selections
        if (files.isEmpty()) return null

        return files.mapNotNull { file ->
            // FileDialog returns File objects directly, already in the correct directory
            validateAndReadFile(file, allowedMimeTypesConfig)
        }.ifEmpty { null }
    }

    private fun validateAndReadFile(file: File, allowedMimeTypesConfig: List<String>): FileData? {
        // Basic validation based on extension if needed, FileDialog's filter does most of it.
        // However, a more robust MIME type check after selection is good.
        val probedMimeType = java.nio.file.Files.probeContentType(file.toPath()) ?: "application/octet-stream"

        val isMimeTypeAllowed = if (allowedMimeTypesConfig.contains("*/*") || allowedMimeTypesConfig.isEmpty()) {
            true
        } else {
            // Check if probedMimeType matches any of the configured general types or specific types
            // This part might need refinement if probedMimeType (e.g. "image/jpeg")
            // needs to be matched against a less specific entry in allowedMimeTypesConfig (e.g. "image/*")
            // For now, direct match or wildcard.
            allowedMimeTypesConfig.contains(probedMimeType) ||
                    allowedMimeTypesConfig.any { it.endsWith("/*") && probedMimeType.startsWith(it.dropLast(1)) }
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
            println("Invalid file type selected on desktop (post-selection check): $probedMimeType (File: ${file.name}). Allowed: ${allowedMimeTypesConfig.joinToString()}")
            null
        }
    }


    actual suspend fun pickSingleFile(allowedTypes: List<String>): FileData? = withContext(Dispatchers.IO) {
        var fileData: FileData? = null
        SwingUtilities.invokeAndWait {
            val dialog = FileDialog(getParentFrame(), "Select File", FileDialog.LOAD)
            dialog.filenameFilter = createFilenameFilter(allowedTypes)
            dialog.isMultipleMode = false
            dialog.isVisible = true

            if (dialog.file != null && dialog.directory != null) {
                val selectedFile = File(dialog.directory, dialog.file)
                fileData = validateAndReadFile(selectedFile, allowedTypes)
            }
        }
        fileData
    }

    actual suspend fun pickMultipleFiles(allowedTypes: List<String>): List<FileData>? = withContext(Dispatchers.IO) {
        var fileDataList: List<FileData>? = null
        SwingUtilities.invokeAndWait {
            val dialog = FileDialog(getParentFrame(), "Select Files", FileDialog.LOAD)
            dialog.filenameFilter = createFilenameFilter(allowedTypes)
            dialog.isMultipleMode = true // Enable multiple file selection
            dialog.isVisible = true

            fileDataList = processFileDialogResult(dialog, allowedTypes)
        }
        fileDataList
    }
}

@Composable
actual fun rememberSingleFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val scope = rememberCoroutineScope()
    val filePicker = remember { FilePicker() } // Instantiate your FilePicker

    return {
        scope.launch { // No need for Dispatchers.IO here if FilePicker methods are suspend and use withContext
            val fileData = filePicker.pickSingleFile(allowedTypes)
            onFilePicked(fileData) // Already on Main dispatcher if pickSingleFile uses withContext(Dispatchers.Main) for callback
        }
    }
}

@Composable
actual fun rememberMultipleFilesPickerLauncher(
    onFilesPicked: (List<FileData>?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val scope = rememberCoroutineScope()
    val filePicker = remember { FilePicker() }

    return {
        scope.launch {
            val fileDataList = filePicker.pickMultipleFiles(allowedTypes)
            onFilesPicked(fileDataList)
        }
    }
}


@Composable
actual fun rememberDirectoryPickerLauncher(
    onDirectoryPicked: (DirectoryUploadRequest?) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()

    return {
        scope.launch { // Launch a coroutine for processing
            val directoryPath = pickDirectoryDesktop()
            if (directoryPath != null) {
                val request = processFileTree(directoryPath)
                onDirectoryPicked(request)
            } else {
                onDirectoryPicked(null) // Selection cancelled
            }
        }
    }
}

private suspend fun pickDirectoryDesktop(): File? = withContext(Dispatchers.IO) {
    var selectedDirectory: File? = null
    // FileDialog is an AWT component, operations should be on the EDT.
    try {
        // For macOS, to enable directory selection in FileDialog:
        System.setProperty("apple.awt.fileDialogForDirectories", "true")

        SwingUtilities.invokeAndWait {
            val parentFrame = getParentFrame() // Your existing helper
            val dialog = FileDialog(parentFrame, "Select Folder to Upload", FileDialog.LOAD)
            // No direct setFileSelectionMode(DIRECTORIES_ONLY) for standard FileDialog.
            // The system property above handles it for macOS.
            // For other OSes, FileDialog's native directory picking capability might vary.

            dialog.isVisible = true

            if (dialog.directory != null && dialog.file != null) {
                // When "apple.awt.fileDialogForDirectories" is true,
                // dialog.getDirectory() + dialog.getFile() usually forms the path to the selected directory.
                // Or, if dialog.getFile() is just the directory name, dialog.getDirectory() is its parent.
                // A more robust way for directory selection might be just File(dialog.getDirectory(), dialog.getFile())
                // if dialog.getDirectory() gives the parent and dialog.getFile() is the directory name itself.
                // If dialog.getDirectory() gives the full path to the directory, and dialog.getFile() is the directory name:
                val selectedPath = File(dialog.directory, dialog.file)
                if (selectedPath.isDirectory) {
                    selectedDirectory = selectedPath
                } else {
                    // Fallback or logging if the selection isn't a directory as expected
                    // This might happen if the system property doesn't work as intended on non-macOS
                    println("FileDialog selection was not a directory: ${selectedPath.absolutePath}")
                    // On some systems, if a directory is "selected", dialog.directory might be the path
                    // and dialog.file might be null or the directory name.
                    // Let's try checking dialog.getDirectory() itself if the above fails.
                    val dirOnlyPath = File(dialog.directory)
                    if (dirOnlyPath.isDirectory) {
                        selectedDirectory = dirOnlyPath
                    } else {
                        println("FileDialog's dialog.getDirectory() was also not a directory: ${dirOnlyPath.absolutePath}")
                    }
                }
            } else if (dialog.directory != null) {
                // Sometimes, for a directory, getFile() might be null and getDirectory() holds the path.
                val dirPath = File(dialog.directory)
                if (dirPath.isDirectory) {
                    selectedDirectory = dirPath
                } else {
                    println("FileDialog only had dialog.getDirectory(), but it wasn't a directory: ${dirPath.absolutePath}")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace() // Handle exceptions during Swing interaction
    } finally {
        // It's good practice to reset the system property if you changed it,
        // though its effect is typically per-dialog instance on macOS.
        // For broader safety or if other FileDialogs are used for files:
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
    }
    selectedDirectory
}


private fun processFileTree(directory: File): DirectoryUploadRequest {
    val filesInDirectory = mutableListOf<FileData>()
    val subDirectoriesList = mutableListOf<DirectoryUploadRequest>()

    directory.listFiles()?.forEach { childFile ->
        // --- Add this condition to skip .DS_Store and other unwanted files ---
        if (childFile.name == ".DS_Store" || childFile.isHidden) { // You can add more specific checks if needed
            // Skip .DS_Store files and other hidden files
            return@forEach // Skips the rest of the code in this iteration of the loop
        }
        // --- End of addition ---

        if (childFile.isDirectory) {
            subDirectoriesList.add(processFileTree(childFile)) // Recurse
        } else if (childFile.isFile) {
            val fileName = childFile.name
            val mimeType = try {
                java.nio.file.Files.probeContentType(childFile.toPath()) ?: "application/octet-stream"
            } catch (e: Exception) {
                "application/octet-stream"
            }
            try {
                val bytes = childFile.readBytes()
                filesInDirectory.add(FileData(fileName, bytes, mimeType))
            } catch (e: Exception) {
                println("Error reading file ${childFile.name}: ${e.message}")
            }
        }
    }
    return DirectoryUploadRequest(
        directoryName = directory.name,
        files = filesInDirectory,
        subDirectories = subDirectoriesList
    )
}