package uz.mobiledv.test1.util

import java.io.File

// ... (existing code in util.kt)

// Expect declaration for opening a file
/**
 * Opens the specified file using the appropriate platform mechanism.
 *
 * @param filePathOnDesktopOrUriStringOnAndroid For Desktop, this is the absolute file path.
 * For Android, this is the String representation of a content URI.
 * @param mimeType The MIME type of the file (e.g., "application/pdf", "image/png"). Crucial for Android.
 */
expect fun openFile(filePath: String, mimeType: String?)


/**
 * Opens the directory containing the specified file in the default file manager.
 *
 * @param context The context needed to start the activity.
 * @param file The file whose containing directory should be opened.
 */
expect fun openFileLocationInFileManager()