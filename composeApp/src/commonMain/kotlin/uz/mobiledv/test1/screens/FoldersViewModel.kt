// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FoldersViewModel.kt
package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.decodeFromString // Ensure this import is present
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.di.BUCKET
import uz.mobiledv.test1.di.DOCUMENT
import uz.mobiledv.test1.di.FOLDER
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.FileData
import uz.mobiledv.test1.util.FileSaver
import com.benasher44.uuid.uuid4


// ... (Keep existing sealed classes: FoldersUiState, FolderDocumentsUiState, FileUploadUiState, FileDownloadUiState) ...
sealed class FoldersUiState {
    data object Idle : FoldersUiState()
    data object Loading : FoldersUiState()
    data class Success(val folders: List<Folder>) : FoldersUiState()
    data class Error(val message: String) : FoldersUiState()
}

sealed class FolderDocumentsUiState {
    data object Idle : FolderDocumentsUiState()
    data object Loading : FolderDocumentsUiState()
    data class Success(val documents: List<Document>) : FolderDocumentsUiState()
    data class Error(val message: String) : FolderDocumentsUiState()
}

sealed class FileUploadUiState {
    data object Idle : FileUploadUiState()
    data object Loading : FileUploadUiState()
    data class Success(val message: String) : FileUploadUiState()
    data class Error(val message: String) : FileUploadUiState()
}

sealed class FileDownloadUiState {
    data object Idle : FileDownloadUiState()
    data object Loading : FileDownloadUiState()
    data class Success(val fileName: String, val message: String) : FileDownloadUiState()
    data class Error(val message: String) : FileDownloadUiState()
}


class FoldersViewModel(
    private val supabaseClient: SupabaseClient,
    private val fileSaver: FileSaver,
    private val authSettings: AuthSettings
) : ViewModel() {

    private val _foldersUiState = MutableStateFlow<FoldersUiState>(FoldersUiState.Idle)
    val foldersUiState: StateFlow<FoldersUiState> = _foldersUiState.asStateFlow()

    private val _folderDocumentsUiState =
        MutableStateFlow<FolderDocumentsUiState>(FolderDocumentsUiState.Idle)
    val folderDocumentsUiState: StateFlow<FolderDocumentsUiState> =
        _folderDocumentsUiState.asStateFlow()

    private val _fileUploadUiState = MutableStateFlow<FileUploadUiState>(FileUploadUiState.Idle)
    val fileUploadUiState: StateFlow<FileUploadUiState> = _fileUploadUiState.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val _fileDownloadUiState =
        MutableStateFlow<FileDownloadUiState>(FileDownloadUiState.Idle)
    val fileDownloadUiState: StateFlow<FileDownloadUiState> =
        _fileDownloadUiState.asStateFlow()


    init {
        if (authSettings.getCurrentUser() != null) {
            loadFolders()
        }
    }

    private fun getCurrentUserId(): String? {
        return authSettings.getCurrentUser()?.id
    }

    private fun isCurrentUserAdmin(): Boolean {
        return authSettings.getCurrentUser()?.isAdmin == true
    }


    fun loadFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            _foldersUiState.value = FoldersUiState.Loading
            if (authSettings.getCurrentUser() == null) {
                _foldersUiState.value =
                    FoldersUiState.Error("User not authenticated to load folders.")
                return@launch
            }

            try {
                val postgrestResponse =
                    supabaseClient.postgrest[FOLDER].select(columns = Columns.ALL) {
                        order("name", Order.ASCENDING)
                    }
                val rawJsonResponse = postgrestResponse.data // Get the raw JSON string
                println("DEBUG: Raw JSON Response for Folders: $rawJsonResponse") // Log the raw JSON


                val folders = postgrestResponse.decodeList<Folder>()

                _foldersUiState.value = FoldersUiState.Success(folders)
            } catch (e: Exception) {
                _foldersUiState.value = FoldersUiState.Error("Error loading folders: ${e.message}")
                _operationStatus.value = "Failed to load folders: ${e.message}"
                println("DEBUG: SerializationException in loadFolders: ${e.localizedMessage}")
                e.printStackTrace() // This will print the full stack trace
            }
        }
    }

    // ... (keep the rest of your FoldersViewModel methods: createFolder, updateFolder, deleteFolder, loadDocumentsForFolder, uploadFileToFolder, downloadFile, etc.)
    // Ensure they are the same as the last version I provided.
    fun createFolder(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to create folder."
                return@launch
            }

            val specificDateTime = "24.05.2025/09:18 AM" // Example fixed date
            val (datePart, timePart) = specificDateTime.split('/')
            val (day, month, year) = datePart.split('.').map { it.toInt() }
            val (hourMinutePart, amPm) = timePart.split(' ')
            val (hour, minute) = hourMinutePart.split(':').map { it.toInt() }

            val adjustedHour = when {
                amPm.equals("PM", ignoreCase = true) && hour != 12 -> hour + 12
                amPm.equals("AM", ignoreCase = true) && hour == 12 -> 0 // Midnight
                else -> hour
            }
            val localDateTime = LocalDateTime(year, month, day, adjustedHour, minute)
            val instant = localDateTime.toInstant(TimeZone.UTC)

            try {
                val newFolder = Folder(
                    id = uuid4().toString(), // Generate ID client-side for folders
                    name = name,
                    description = description,
                    userId = currentUserId, // Associated with the logged-in user
                    createdAt = instant.toString(),
                )
                supabaseClient.postgrest[FOLDER].insert(newFolder)
                _operationStatus.value = "Folder '$name' created successfully."
                loadFolders()
            } catch (e: Exception) {
                _operationStatus.value = "Error creating folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun updateFolder(folderId: String, name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to update folder."
                return@launch
            }
            if (!isCurrentUserAdmin()) {
                _operationStatus.value = "Only designated managers can update folders."
                return@launch
            }
            try {
                supabaseClient.postgrest[FOLDER]
                    .update(
                        {
                            set("name", name)
                            set("description", description)
                        }
                    ) {
                        filter {
                            eq("id", folderId)
                        }
                    }
                _operationStatus.value = "Folder '$name' updated successfully."
                loadFolders()
            } catch (e: Exception) {
                _operationStatus.value = "Error updating folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to delete folder."
                return@launch
            }
            if (!isCurrentUserAdmin()) {
                _operationStatus.value = "Only designated managers can delete folders."
                return@launch
            }
            try {
                supabaseClient.postgrest[FOLDER]
                    .delete {
                        filter { eq("id", folderId) }
                    }
                _operationStatus.value = "Folder deleted successfully."
                loadFolders()
            } catch (e: Exception) {
                _operationStatus.value = "Error deleting folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun loadDocumentsForFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _folderDocumentsUiState.value = FolderDocumentsUiState.Loading
            if (authSettings.getCurrentUser() == null) {
                _folderDocumentsUiState.value =
                    FolderDocumentsUiState.Error("User not authenticated to load documents.")
                return@launch
            }
            try {
                val documents = supabaseClient.postgrest[DOCUMENT]
                    .select {
                        filter {
                            eq("folder_id", folderId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Document>()
                _folderDocumentsUiState.value = FolderDocumentsUiState.Success(documents)
            } catch (e: Exception) {
                _folderDocumentsUiState.value =
                    FolderDocumentsUiState.Error("Error loading documents: ${e.message}")
                _operationStatus.value = "Failed to load documents: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun uploadFileToFolder(folderId: String, fileData: FileData) {
        viewModelScope.launch(Dispatchers.IO) {
            _fileUploadUiState.value = FileUploadUiState.Loading
            val currentUserId = getCurrentUserId() ?: run {
                _fileUploadUiState.value =
                    FileUploadUiState.Error("User not authenticated to upload.")
                return@launch
            }
            if (!isCurrentUserAdmin()) {
                _fileUploadUiState.value =
                    FileUploadUiState.Error("Only designated managers can upload files.")
                return@launch
            }

            val storagePath = "${currentUserId}/folder_${folderId}/${uuid4()}_${fileData.name}"

            try {
                supabaseClient.storage[BUCKET].upload(
                    path = storagePath,
                    data = fileData.bytes,
                    options = { upsert = false }
                )

                val documentMetadata = Document(
                    id = uuid4().toString(),
                    folderId = folderId,
                    name = fileData.name,
                    storageFilePath = storagePath,
                    userId = currentUserId,
                    mimeType = fileData.mimeType,
                    createdAt = Clock.System.now().toString()
                )

                supabaseClient.postgrest[DOCUMENT].insert(documentMetadata)

                _fileUploadUiState.value =
                    FileUploadUiState.Success("File '${fileData.name}' uploaded.")
                loadDocumentsForFolder(folderId)
            } catch (e: Exception) {
                _fileUploadUiState.value = FileUploadUiState.Error("Upload failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun downloadFile(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            if (document.storageFilePath == null) {
                _fileDownloadUiState.value = FileDownloadUiState.Error("File path is missing.")
                return@launch
            }

            _fileDownloadUiState.value = FileDownloadUiState.Loading
            try {
                val bytes =
                    supabaseClient.storage[BUCKET].downloadPublic(document.storageFilePath!!)

                val fileName = document.name
                val mimeType = document.mimeType ?: "application/octet-stream"

                val success = fileSaver.saveFile(FileData(fileName, bytes, mimeType))
                if (success) {
                    _fileDownloadUiState.value =
                        FileDownloadUiState.Success(fileName, "File '$fileName' downloaded.")
                } else {
                    _fileDownloadUiState.value =
                        FileDownloadUiState.Error("Failed to save file '$fileName'.")
                }
            } catch (e: Exception) {
                _fileDownloadUiState.value =
                    FileDownloadUiState.Error("Download failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun clearFileDownloadStatus() {
        _fileDownloadUiState.value = FileDownloadUiState.Idle
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to delete document."
                return@launch
            }
            if (!isCurrentUserAdmin()) {
                _operationStatus.value = "Only designated managers can delete documents."
                return@launch
            }

            if (document.id.isBlank()) {
                _operationStatus.value = "Cannot delete document: Missing ID."
                return@launch
            }

            try {
                supabaseClient.postgrest[DOCUMENT].delete {
                    filter { eq("id", document.id) }
                }

                document.storageFilePath?.let {
                    kotlin.runCatching { supabaseClient.storage[BUCKET].delete(listOf(it)) }
                        .onFailure { e -> println("Failed to delete from storage (might be already deleted or permissions): ${e.message}") }
                }

                _operationStatus.value = "Document '${document.name}' deleted successfully."
                loadDocumentsForFolder(document.folderId)
            } catch (e: Exception) {
                _operationStatus.value = "Error deleting document '${document.name}': ${e.message}"
                e.printStackTrace()
            }
        }
    }


    fun clearFileUploadStatus() {
        _fileUploadUiState.value = FileUploadUiState.Idle
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}