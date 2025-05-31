package uz.mobiledv.test1.util

expect class FileSaver {
    suspend fun saveFile(fileData: FileData): String?

    suspend fun saveFileToPublicDownloads(fileData: FileData): String?
}

/**
 * Expected class for platform-specific file picking and saving operations.
 * The 'context' parameter type is Any? to accommodate different platform requirements
 * (e.g., ComponentActivity on Android, Frame or null on Desktop).
 * It's the responsibility of the platform-specific DI or instantiation logic
 * to provide the correct context type.
 */