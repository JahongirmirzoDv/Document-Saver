package uz.mobiledv.test1.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns // Not directly used for opening, but ok
import androidx.activity.compose.rememberLauncherForActivityResult // Not used here
import androidx.activity.result.contract.ActivityResultContracts // Not used here
import androidx.compose.runtime.Composable // Not used here
import androidx.compose.ui.platform.LocalContext // Not used here
import androidx.core.content.FileProvider
import uz.mobiledv.test1.MyActivity // For application context
import java.io.File

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