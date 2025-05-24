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
import uz.mobiledv.test1.di.BUCKET
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.FileData

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


class FoldersViewModel(
    private val supabaseClient: SupabaseClient
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
                val folders = supabaseClient.postgrest["folders"]
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
            try {
                val newFolder = Folder(
                    name = name,
                    description = description,
                    userId = currentUserId
                    // Supabase handles 'id' and 'created_at'
                )
                supabaseClient.postgrest["folders"].insert(newFolder, request = {}) // Explicitly no upsert
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
                supabaseClient.postgrest["folders"]
                    .update(
                        {
                            set("name", name)
                            set("description", description)
                        }
                    ) {
                        filter{
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
                supabaseClient.postgrest["folders"]
                    .delete {
                        filter{
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
                _folderDocumentsUiState.value = FolderDocumentsUiState.Error("User not authenticated.")
                return@launch
            }
            try {
                val documents = supabaseClient.postgrest["documents"]
                    .select {
                        filter{
                            "folder_id"
                            FilterOperator.EQ
                            folderId
                        }
                        // Optionally filter by user_id if documents also have a direct user_id link
                        // and RLS isn't solely relied upon for this query.
                        // filter("user_id", FilterOperator.EQ, currentUserId)
                        order("name", Order.ASCENDING)
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
                    options = { upsert = false } // Don't overwrite by default, or set to true if needed
                )

                val documentMetadata = Document(
                    folderId = folderId,
                    name = fileData.name,
                    storageFilePath = storagePath,
                    userId = currentUserId,
                    mimeType = fileData.mimeType,
                    createdAt = Clock.System.now().toString() // Supabase can also handle this with a default value
                )
                supabaseClient.postgrest["documents"].insert(documentMetadata)

                _fileUploadUiState.value = FileUploadUiState.Success("File '${fileData.name}' uploaded.")
                // No need to call loadDocumentsForFolder here, FolderDetailScreen will react to state
            } catch (e: Exception) {
                _fileUploadUiState.value = FileUploadUiState.Error("Upload failed: ${e.message}")
                e.printStackTrace()
                // Consider deleting from storage if DB insert fails:
                // kotlin.runCatching { supabaseClient.storage[BUCKET].delete(listOf(storagePath)) }
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