package uz.mobiledv.test1.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns // Not directly used for opening, but ok
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult // Not used here
import androidx.activity.result.contract.ActivityResultContracts // Not used here
import androidx.compose.runtime.Composable // Not used here
import androidx.compose.ui.platform.LocalContext // Not used here
import androidx.core.content.FileProvider
import uz.mobiledv.test1.MyActivity // For application context
import java.io.File
import androidx.core.net.toUri

actual fun openFile(filePath: String, mimeType: String?) {
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
        uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Authority
            file
        )
    } catch (e: Exception) {
        println("Error getting URI from FileProvider: ${e.message}") // This was the previous error
        e.printStackTrace()
        return
    }

    println("Context class: ${context::class.java.name}")
    println("PackageManager: ${context.packageManager}")

    // It's good practice to check if the URI is null, though FileProvider.getUriForFile
    // usually throws an exception rather than returning null on failure.
    // if (uri == null) {
    //     println("Error: FileProvider returned null URI for path: $filePath")
    //     return
    // }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        // setDataAndType(uri, mimeType ?: "*/*") // Potential issue here
        setDataAndType(uri, mimeType) // More specific, if mimeType is guaranteed
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Potentially add FLAG_GRANT_WRITE_URI_PERMISSION if some apps might need it, though less common for ACTION_VIEW
    }

    val chooser = Intent.createChooser(intent, "Open with").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 👈 Required if using application context
    }

    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}

@SuppressLint("QueryPermissionsNeeded")
actual fun openFileLocationInFileManager() {
    val context = MyActivity.AppContextHolder.appContext
    val downloadsUri: Uri =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path.toUri()

    val intent = Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        setDataAndType(downloadsUri, "*/*") // Use a generic MIME type for directories
    }

    // Verify that there's an app available to handle this intent
    if (intent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // This catch block is a safety net, though resolveActivity should prevent this.
            Toast.makeText(
                context,
                "No app found to open the download directory.",
                Toast.LENGTH_SHORT
            ).show()
        }
    } else {
        Toast.makeText(context, "No file manager found.", Toast.LENGTH_SHORT).show()
    }
}