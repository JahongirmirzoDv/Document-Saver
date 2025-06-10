package uz.mobiledv.test1.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Note: A full implementation of file pickers requires platform-specific UI code.
// The approach here provides a placeholder. For a real app, you would typically
// use a library or a custom interface to call native iOS code from your shared module.

actual class FilePicker {
    actual suspend fun pickSingleFile(allowedTypes: List<String>): FileData? {
        println("FilePicker.pickSingleFile() not implemented for iOS yet.")
        return null
    }

    actual suspend fun pickMultipleFiles(allowedTypes: List<String>): List<FileData>? {
        println("FilePicker.pickMultipleFiles() not implemented for iOS yet.")
        return null
    }
}

@Composable
actual fun rememberSingleFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Main)
    return {
        scope.launch {
            println("rememberSingleFilePickerLauncher not implemented for iOS yet.")
            onFilePicked(null)
        }
    }
}

@Composable
actual fun rememberMultipleFilesPickerLauncher(
    onFilesPicked: (List<FileData>?) -> Unit,
    allowedTypes: List<String>
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Main)
    return {
        scope.launch {
            println("rememberMultipleFilesPickerLauncher not implemented for iOS yet.")
            onFilesPicked(null)
        }
    }
}

@Composable
actual fun rememberDirectoryPickerLauncher(
    onDirectoryPicked: (DirectoryUploadRequest?) -> Unit
): () -> Unit {
     return {
        println("rememberDirectoryPickerLauncher not implemented for iOS yet.")
        onDirectoryPicked(null)
    }
}