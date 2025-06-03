package uz.mobiledv.test1.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual class FilePicker {
    actual suspend fun pickSingleFile(allowedTypes: List<String>): FileData? {
        // This direct suspend fun is not the typical way with Compose,
        // prefer using rememberSingleFilePickerLauncher.
        // For brevity, its implementation is omitted here but would require a different approach
        // if needed outside Composable context (e.g., involving an Activity result listener).
        println("Warning: pickSingleFile() called directly. Use rememberSingleFilePickerLauncher in Composables.")
        return null
    }

    actual suspend fun pickMultipleFiles(allowedTypes: List<String>): List<FileData>? {
        // Similar to pickSingleFile, direct suspend usage is complex.
        println("Warning: pickMultipleFiles() called directly. Use rememberMultipleFilesPickerLauncher in Composables.")
        return null
    }
}

@Composable
actual fun rememberSingleFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                processUri(context, uri, allowedTypes, onFilePicked)
            } else {
                onFilePicked(null)
            }
        }
    )

    return {
        try {
            launcher.launch(allowedTypes.toTypedArray())
        } catch (e: Exception) {
            e.printStackTrace()
            onFilePicked(null) // Handle exception during launch
        }
    }
}

@Composable
actual fun rememberMultipleFilesPickerLauncher(
    onFilesPicked: (List<FileData>?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri>? ->
            if (!uris.isNullOrEmpty()) {
                val fileDataList = mutableListOf<FileData>()
                var allFilesValid = true
                for (uri in uris) {
                    processUri(context, uri, allowedTypes) { fileData ->
                        if (fileData != null) {
                            fileDataList.add(fileData)
                        } else {
                            allFilesValid = false // Mark if any file is invalid
                        }
                    }
                }
                if (fileDataList.isNotEmpty()) {
                    onFilesPicked(fileDataList)
                } else if (!allFilesValid) { // Some files were invalid, but none were valid
                    onFilesPicked(null) // Indicate overall failure or partial failure
                } else { // No uris processed successfully, or list was empty initially
                    onFilesPicked(null)
                }
            } else {
                onFilesPicked(null)
            }
        }
    )

    return {
        try {
            launcher.launch(allowedTypes.toTypedArray())
        } catch (e: Exception) {
            e.printStackTrace()
            onFilesPicked(null) // Handle exception during launch
        }
    }
}

// Helper function to process a URI and extract FileData
private fun processUri(
    context: Context,
    uri: Uri,
    allowedMimeTypesConfig: List<String>, // Mime types passed to the picker
    onResult: (FileData?) -> Unit
) {
    try {
        val contentResolver = context.contentResolver
        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "unknown_file_${System.currentTimeMillis()}"

        val mimeType = contentResolver.getType(uri)

        // Validate MIME type against the ones used to launch the picker or a predefined list.
        // The `allowedTypes` in `remember...Launcher` are what `ActivityResultContracts` uses.
        // Here we re-verify, as the system picker might sometimes allow selection outside the strict filter.
        val isMimeTypeAllowed = if (allowedMimeTypesConfig.contains("*/*") || allowedMimeTypesConfig.any { it.startsWith("*") }) {
            true // If wildcard is present, assume any type is allowed by the filter
        } else {
            mimeType != null && allowedMimeTypesConfig.contains(mimeType)
        }

        if (isMimeTypeAllowed) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                onResult(FileData(fileName, bytes, mimeType))
            } ?: onResult(null)
        } else {
            println("Invalid file type selected on Android: $mimeType. Allowed: ${allowedMimeTypesConfig.joinToString()}")
            onResult(null) // Notify of invalid file type
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(null)
    }
}


@Composable
actual fun rememberDirectoryPickerLauncher(
    onDirectoryPicked: (DirectoryUploadRequest?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri: Uri? ->
            if (treeUri != null) {
                scope.launch { // Launch a coroutine for processing
                    val request = processDocumentFileTree(context, treeUri)
                    onDirectoryPicked(request)
                }
            } else {
                onDirectoryPicked(null) // Selection cancelled
            }
        }
    )
    return { launcher.launch(null) } // Initial URI can be null to start at default location
}

private suspend fun processDocumentFileTree(context: Context, treeUri: Uri): DirectoryUploadRequest? {
    return withContext(Dispatchers.IO) { // Perform file operations off the main thread
        val rootDocumentFile = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDocumentFile != null && rootDocumentFile.isDirectory) {
            buildDirectoryRequest(context, rootDocumentFile)
        } else {
            null
        }
    }
}

private fun buildDirectoryRequest(context: Context, documentFile: DocumentFile): DirectoryUploadRequest {
    val filesInDirectory = mutableListOf<FileData>()
    val subDirectoriesList = mutableListOf<DirectoryUploadRequest>()

    documentFile.listFiles().forEach { childDocFile ->
        if (childDocFile.isDirectory) {
            subDirectoriesList.add(buildDirectoryRequest(context, childDocFile)) // Recurse
        } else if (childDocFile.isFile) {
            val fileName = childDocFile.name ?: "unknown_file_${System.currentTimeMillis()}"
            val mimeType = context.contentResolver.getType(childDocFile.uri)
            try {
                context.contentResolver.openInputStream(childDocFile.uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    filesInDirectory.add(FileData(fileName, bytes, mimeType))
                }
            } catch (e: Exception) {
                println("Error reading file ${childDocFile.name}: ${e.message}")
                // Optionally, inform the user or skip this file
            }
        }
    }

    return DirectoryUploadRequest(
        directoryName = documentFile.name ?: "Unnamed Directory",
        files = filesInDirectory,
        subDirectories = subDirectoriesList
    )
}