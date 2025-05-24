package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import uz.mobiledv.test1.di.BUCKET
import uz.mobiledv.test1.di.DOCUMENT
import uz.mobiledv.test1.di.FOLDER
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.FileData
import uz.mobiledv.test1.util.FileSaver

// Sealed class to represent UI State for Folders
sealed class FoldersUiState {
    data object Idle : FoldersUiState()
    data object Loading : FoldersUiState()
    data class Success(val folders: List<Folder>) : FoldersUiState()
    data class Error(val message: String) : FoldersUiState()
}

// Sealed class to represent UI State for Documents within a Folder
sealed class FolderDocumentsUiState {
    data object Idle : FolderDocumentsUiState()
    data object Loading : FolderDocumentsUiState()
    data class Success(val documents: List<Document>) : FolderDocumentsUiState()
    data class Error(val message: String) : FolderDocumentsUiState()
}

// Sealed class for File Upload UI State
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
    private val fileSaver: FileSaver, // Injected FileSaver
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
        MutableStateFlow<FileDownloadUiState>(FileDownloadUiState.Idle) // NEW
    val fileDownloadUiState: StateFlow<FileDownloadUiState> =
        _fileDownloadUiState.asStateFlow() // NEW


    init {
        loadFolders()
    }

    private fun getCurrentUserId(): String? {
        val id = supabaseClient.auth.currentUserOrNull()?.id
        if (id == null) {
            _operationStatus.value = "User not authenticated."
        }
        return id
    }

    fun loadFolders() {
        viewModelScope.launch {
            _foldersUiState.value = FoldersUiState.Loading
            val currentUserId = getCurrentUserId() ?: run {
                _foldersUiState.value = FoldersUiState.Error("User not authenticated.")
                return@launch
            }
            try {
                val folders = supabaseClient.postgrest[FOLDER]
                    .select(columns = Columns.ALL) {
                        filter {
                            "user_id"
                            FilterOperator.EQ
                            currentUserId
                        }
                        order("name", Order.ASCENDING)
                    }
                    .decodeList<Folder>()
                _foldersUiState.value = FoldersUiState.Success(folders)
            } catch (e: Exception) {
                _foldersUiState.value = FoldersUiState.Error("Error loading folders: ${e.message}")
                _operationStatus.value = "Failed to load folders: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun createFolder(name: String, description: String) {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId() ?: return@launch

            // Specific date and time
            val specificDateTime = "24.05.2025/09:18 AM"
            val (datePart, timePart) = specificDateTime.split('/')
            val (day, month, year) = datePart.split('.').map { it.toInt() }
            val (hour, minute) = timePart.substring(0, timePart.length - 3).split(':')
                .map { it.toInt() }
            val amPm = timePart.substring(timePart.length - 2)
            val adjustedHour =
                if (amPm == "PM" && hour != 12) hour + 12 else if (amPm == "AM" && hour == 12) 0 else hour
            val localDateTime = LocalDateTime(year, month, day, adjustedHour, minute)
            val instant = localDateTime.toInstant(TimeZone.UTC) // Assuming UTC for Supabase

            try {
                val newFolder = Folder(
                    name = name,
                    description = description,
                    userId = currentUserId,
                    createdAt = instant.toString(), // Set the specific date and time
                    // Supabase handles 'id' and 'created_at'
                )
                supabaseClient.postgrest[FOLDER].insert(
                    newFolder,
                    request = {}) // Explicitly no upsert
                _operationStatus.value = "Folder '$name' created successfully."
                loadFolders()
            } catch (e: Exception) {
                _operationStatus.value = "Error creating folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun updateFolder(folderId: String, name: String, description: String) {
        viewModelScope.launch {
            getCurrentUserId() ?: return@launch // Ensure user is authenticated
            try {
                supabaseClient.postgrest[FOLDER]
                    .update(
                        {
                            set("name", name)
                            set("description", description)
                        }
                    ) {
                        filter {
                            "id"
                            FilterOperator.EQ
                            folderId
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
        viewModelScope.launch {
            getCurrentUserId() ?: return@launch
            try {
                // Consider deleting documents within the folder from storage and DB first
                // For now, just deleting the folder record. Add cascade delete in Supabase DB or handle here.
                supabaseClient.postgrest[FOLDER]
                    .delete {
                        filter {
                            "id"
                            FilterOperator.EQ
                            folderId
                        }
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
        viewModelScope.launch {
            _folderDocumentsUiState.value = FolderDocumentsUiState.Loading
            val currentUserId = getCurrentUserId() ?: run {
                _folderDocumentsUiState.value =
                    FolderDocumentsUiState.Error("User not authenticated.")
                return@launch
            }
            try {
                val documents = supabaseClient.postgrest[DOCUMENT]
                    .select {
                        filter {
                            eq("folder_id", folderId) // Ensure we filter by folderId
                        }
                        order("created_at", Order.DESCENDING) // Order by creation date
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
        viewModelScope.launch {
            _fileUploadUiState.value = FileUploadUiState.Loading
            val currentUserId = getCurrentUserId() ?: run {
                _fileUploadUiState.value = FileUploadUiState.Error("User not authenticated.")
                return@launch
            }

            val storagePath = "${currentUserId}/${folderId}/${fileData.name}"

            try {
                supabaseClient.storage[BUCKET].upload(
                    path = storagePath,
                    data = fileData.bytes,
                    options = {
                        upsert = false
                    } // Don't overwrite by default, or set to true if needed
                )

                val documentMetadata = Document(
                    folderId = folderId,
                    name = fileData.name,
                    storageFilePath = storagePath,
                    userId = currentUserId,
                    mimeType = fileData.mimeType,
                    createdAt = Clock.System.now()
                        .toString() // Supabase can also handle this with a default value
                )
                supabaseClient.postgrest[DOCUMENT].insert(documentMetadata)

                _fileUploadUiState.value =
                    FileUploadUiState.Success("File '${fileData.name}' uploaded.")
                // No need to call loadDocumentsForFolder here, FolderDetailScreen will react to state
            } catch (e: Exception) {
                _fileUploadUiState.value = FileUploadUiState.Error("Upload failed: ${e.message}")
                e.printStackTrace()
                // Consider deleting from storage if DB insert fails:
                // kotlin.runCatching { supabaseClient.storage[BUCKET].delete(listOf(storagePath)) }
            }
        }
    }

    fun downloadFile(document: Document) {
        viewModelScope.launch {
            if (document.storageFilePath == null) {
                _fileDownloadUiState.value = FileDownloadUiState.Error("File path is missing.")
                return@launch
            }
            _fileDownloadUiState.value = FileDownloadUiState.Loading
            try {
                val bytes =
                    supabaseClient.storage[BUCKET].downloadAuthenticated(document.storageFilePath!!) // Use downloadAuthenticated
                // Now save the file using platform-specific code
                val fileName = document.name // Or extract from storageFilePath if more reliable
                val mimeType = document.mimeType ?: "application/octet-stream"

                fileSaver.saveFile(FileData(fileName, bytes, mimeType)) // Use injected FileSaver

                _fileDownloadUiState.value =
                    FileDownloadUiState.Success(fileName, "File '$fileName' downloaded.")
            } catch (e: Exception) {
                _fileDownloadUiState.value =
                    FileDownloadUiState.Error("Download failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun clearFileDownloadStatus() { // NEW
        _fileDownloadUiState.value = FileDownloadUiState.Idle
    }

    fun deleteDocument(document: Document) { // NEW for Managers
        viewModelScope.launch {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to delete document."
                return@launch
            }
            if (document.storageFilePath == null && document.id.isBlank()) {
                _operationStatus.value = "Cannot delete document: Missing ID or storage path."
                return@launch
            }

            try {
                // 1. Delete from Postgrest table
                supabaseClient.postgrest[DOCUMENT].delete {
                    filter {
                        eq("id", document.id) // Use eq for equality check
                    }
                }

                // 2. Delete from Supabase Storage (if storageFilePath exists)
                document.storageFilePath?.let {
                    supabaseClient.storage[BUCKET].delete(listOf(it))
                }

                _operationStatus.value = "Document '${document.name}' deleted successfully."
                // Refresh documents list for the current folder
                (_folderDocumentsUiState.value as? FolderDocumentsUiState.Success)?.let { currentState ->
                    val currentFolderId = currentState.documents.firstOrNull()?.folderId
                    if (currentFolderId != null && currentFolderId.isNotBlank()) {
                        loadDocumentsForFolder(currentFolderId)
                    } else {
                        // If we can't get folderId from current state, try to find another way or prompt refresh.
                        // For now, we rely on user navigating back or manual refresh.
                        // Consider passing folderId to deleteDocument if it's always available.
                    }
                }


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