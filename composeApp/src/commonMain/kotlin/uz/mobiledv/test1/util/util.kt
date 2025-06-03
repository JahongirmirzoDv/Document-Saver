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

// Expect declaration for the file picker class
expect class FilePicker {
    suspend fun pickSingleFile(allowedTypes: List<String> = listOf("*/*")): FileData?
    suspend fun pickMultipleFiles(allowedTypes: List<String> = listOf("*/*")): List<FileData>?
}

data class DirectoryUploadRequest(
    val directoryName: String,
    val files: List<FileData>, // FileData is your existing class
    val subDirectories: List<DirectoryUploadRequest>
)

// Expect declaration for composable single file picker launcher
@Composable
expect fun rememberSingleFilePickerLauncher(
    onFilePicked: (FileData?) -> Unit,
    allowedTypes: List<String> = listOf(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "application/msword", // .doc
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
        "application/vnd.ms-excel", // .xls
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
    )
): () -> Unit

// Expect declaration for composable multiple files picker launcher
@Composable
expect fun rememberMultipleFilesPickerLauncher(
    onFilesPicked: (List<FileData>?) -> Unit,
    allowedTypes: List<String> = listOf(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "application/msword", // .doc
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
        "application/vnd.ms-excel", // .xls
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
    )
): () -> Unit

@Composable
expect fun rememberDirectoryPickerLauncher(
    onDirectoryPicked: (DirectoryUploadRequest?) -> Unit
): () -> Unit