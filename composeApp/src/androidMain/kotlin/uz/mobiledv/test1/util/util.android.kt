package uz.mobiledv.test1.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Data class FileData remains the same

actual class FilePicker {
    actual suspend fun pickFile(allowedTypes: List<String>): FileData? {
        println("Warning: The suspend fun pickFile() is not the recommended way to pick files on Android with Compose. Please use rememberFilePickerLauncher.")
        return null
    }
}

@Composable
actual fun rememberFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit
): () -> Unit { // Return type is now () -> Unit, matching the expect declaration
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    val contentResolver = context.contentResolver
                    val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "unknown_file_${System.currentTimeMillis()}"

                    val mimeType = contentResolver.getType(uri)

                    // Validate MIME type
                    val allowedMimeTypes = listOf(
                        "application/pdf",
                        "image/png",
                        "image/jpeg",
                        "application/msword", // .doc
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                        "application/vnd.ms-excel", // .xls
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
                    )

                    if (mimeType != null && allowedMimeTypes.contains(mimeType)) {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            onFilePicked(FileData(fileName, bytes, mimeType))
                        } ?: onFilePicked(null)
                    } else {
                        // Notify user of invalid file type
                        onFilePicked(null) // Or pass a specific error
                        // You could show a Toast or Snackbar here, e.g., by calling a callback
                        println("Invalid file type selected: $mimeType")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onFilePicked(null)
                }
            } else {
                onFilePicked(null)
            }
        }
    )

    // Return a lambda that takes no arguments
    return {
        // Define the MIME types for the OpenDocument contract
        val mimeTypesToLaunch = arrayOf(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
        )

        try {
            launcher.launch(mimeTypesToLaunch)
        } catch (e: Exception) {
            e.printStackTrace()
            onFilePicked(null) // Handle exception during launch
        }
    }
}