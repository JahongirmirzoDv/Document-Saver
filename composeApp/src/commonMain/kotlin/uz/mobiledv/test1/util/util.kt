package uz.mobiledv.test1.util

import androidx.compose.runtime.Composable

// A reasonably common regex for email validation.
// For more robust validation, consider a library or more complex regex.
private val EMAIL_ADDRESS_REGEX =
    Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && EMAIL_ADDRESS_REGEX.matches(email)
}


// Data class to hold file information
data class FileData(val name: String, val bytes: ByteArray, val mimeType: String?)

// Expect declaration for the file picker
expect class FilePicker {
    suspend fun pickFile(allowedTypes: List<String> = listOf("*/*")): FileData?
}

// You might also provide a Composable way to launch it
@Composable
expect fun rememberFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit
): () -> Unit