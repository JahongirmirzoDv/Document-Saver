package uz.mobiledv.test1.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import uz.mobiledv.test1.MyActivity // For application context
import java.io.File // Import java.io.File

// FileData class, FilePicker class, and rememberFilePickerLauncher remain the same as your existing code


actual fun openFile(filePath: String, mimeType: String?) { // Parameter name reflects it's a path
    val context = MyActivity.AppContextHolder.appContext
    if (!MyActivity.AppContextHolder.isInitialized()) {
        println("Error: Application context not available for opening file.")
        return
    }

    val file = File(filePath)
    if (!file.exists()) {
        println("Error: File not found at path: $filePath")
        return
    }

    val uri: Uri
    try {
        // Use FileProvider to get a content URI
        uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Authority must match AndroidManifest
            file
        )
    } catch (e: Exception) {
        println("Error getting URI from FileProvider: ${e.message}")
        e.printStackTrace()
        return
    }

    if (uri == null) {
        println("Error: FileProvider returned null URI for path: $filePath")
        return
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: "*/*") // Use provided mimeType or a generic one
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            println("Error: No application found to open file with URI $uri and MIME type $mimeType")
            // Optionally show a toast or snackbar to the user:
            // android.widget.Toast.makeText(context, "No app found to open this file type.", android.widget.Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        println("Error starting activity to open file: ${e.message}")
        e.printStackTrace()
    }
}