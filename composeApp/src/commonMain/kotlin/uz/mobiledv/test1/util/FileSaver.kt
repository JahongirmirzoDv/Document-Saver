package uz.mobiledv.test1.util

expect class FileSaver {
    suspend fun saveFile(fileData: FileData): Boolean
}