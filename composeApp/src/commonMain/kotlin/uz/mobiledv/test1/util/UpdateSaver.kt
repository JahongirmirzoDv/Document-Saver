package uz.mobiledv.test1.util

import okio.Sink

/**
 * Expect class for platform-specific file saving operations.
 * Implementations should handle creating the file and providing a Sink to write to.
 */
expect class UpdateSaver {
    /**
     * Saves a file with the given name in the specified target directory.
     * The actual writing is done by the [writer] lambda, which receives a [Sink].
     *
     * @param fileName The name of the file to save.
     * @param targetDirectory A platform-specific representation of the target directory path
     * (e.g., "apk_updates" which might map to cacheDir/apk_updates).
     * @param writer A suspend lambda that takes a [Sink] and writes data to it.
     * @return The platform-specific absolute path to the saved file as a String, or null on failure.
     */
    suspend fun saveFile(fileName: String, targetDirectory: String, writer: suspend (sink: Sink) -> Unit): String?
}
