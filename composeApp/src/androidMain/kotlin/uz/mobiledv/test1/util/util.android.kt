package uz.mobiledv.test1.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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