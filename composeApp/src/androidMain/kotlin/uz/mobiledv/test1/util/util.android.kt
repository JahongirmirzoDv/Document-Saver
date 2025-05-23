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

                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        onFilePicked(FileData(fileName, bytes, mimeType))
                    } ?: onFilePicked(null)
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
        // You define the MIME types to be used here.
        // For example, to allow any file type:
        val mimeTypesToLaunch = arrayOf("*/*")
        // Or, for specific types:
        // val mimeTypesToLaunch = arrayOf("image/png", "application/pdf")

        try {
            launcher.launch(mimeTypesToLaunch)
        } catch (e: Exception) {
            e.printStackTrace()
            onFilePicked(null)
        }
    }
}