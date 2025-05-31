// File: jahongirmirzodv/test.1.2/Test.1.2-fcc101c924a3dcb58258c4f63c298289470731ad/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FoldersViewModel.kt
package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
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
// Removed unused kotlinx.datetime imports for LocalDateTime, TimeZone, toInstant
import uz.mobiledv.test1.data.AuthSettings // AuthSettings for user context
import uz.mobiledv.test1.di.BUCKET
import uz.mobiledv.test1.di.DOCUMENT
import uz.mobiledv.test1.di.FOLDER
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.FileData
import uz.mobiledv.test1.util.FileSaver
import uz.mobiledv.test1.util.openFile


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

// Existing state for downloading to cache and opening
sealed class FileDownloadUiState {
    data object Idle : FileDownloadUiState()
    data object Loading : FileDownloadUiState() // General loading state
    data class Downloading(val fileName: String, val progress: Float) : FileDownloadUiState() // Progress state
    data class Success(val fileName: String, val message: String, val localPath: String, val mimeType: String?) : FileDownloadUiState()
    data class Error(val message: String) : FileDownloadUiState()
}

// New state for downloading to public "Downloads" folder
sealed class FilePublicDownloadUiState {
    data object Idle : FilePublicDownloadUiState()
    data class Downloading(val fileName: String, val progress: Float) : FilePublicDownloadUiState()
    data class Success(val fileName: String, val message: String, val publicPath: String?) : FilePublicDownloadUiState()
    data class Error(val message: String) : FilePublicDownloadUiState()
}


class FoldersViewModel(
    private val supabaseClient: SupabaseClient,
    private val fileSaver: FileSaver,
    private val authSettings: AuthSettings // Injected AuthSettings
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

    private val _fileDownloadUiState = // For opening/viewing (downloads to cache)
        MutableStateFlow<FileDownloadUiState>(FileDownloadUiState.Idle)
    val fileDownloadUiState: StateFlow<FileDownloadUiState> =
        _fileDownloadUiState.asStateFlow()

    // New State Flow for public downloads
    private val _filePublicDownloadUiState =
        MutableStateFlow<FilePublicDownloadUiState>(FilePublicDownloadUiState.Idle)
    val filePublicDownloadUiState: StateFlow<FilePublicDownloadUiState> =
        _filePublicDownloadUiState.asStateFlow()


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
            if (getCurrentUserId() == null) { // Check custom auth status
                _foldersUiState.value =
                    FoldersUiState.Error("User not authenticated to load folders.")
                return@launch
            }

            try {
                val postgrestResponse =
                    supabaseClient.postgrest[FOLDER].select(columns = Columns.ALL) {
                        order("name", Order.ASCENDING)
                    }
                val folders = postgrestResponse.decodeList<Folder>()
                _foldersUiState.value = FoldersUiState.Success(folders)
            } catch (e: Exception) {
                _foldersUiState.value = FoldersUiState.Error("Error loading folders: ${e.message}")
                _operationStatus.value = "Failed to load folders: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun createFolder(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to create folder."
                return@launch
            }
            try {
                val newFolder = Folder(
                    id = uuid4().toString(),
                    name = name,
                    description = description,
                    userId = currentUserId,
                    createdAt = Clock.System.now().toString(),
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
            getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated."
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
            getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated."
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
            if (getCurrentUserId() == null) {
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
                _fileUploadUiState.value = FileUploadUiState.Error("User not authenticated to upload.")
                return@launch
            }
            if (!isCurrentUserAdmin()) {
                _fileUploadUiState.value = FileUploadUiState.Error("Only designated managers can upload files.")
                return@launch
            }

            val originalFileName = fileData.name
            val fileExtension = originalFileName.substringAfterLast('.', "").trim().lowercase()
            val safeExtension = if (fileExtension.matches(Regex("^[a-zA-Z0-9]+$")) && fileExtension.length <= 5) {
                ".$fileExtension"
            } else {
                ""
            }
            val uniqueStorageObjectName = "${uuid4()}$safeExtension"
            val storagePath = "${currentUserId}/folder_${folderId}/${uniqueStorageObjectName}"

            try {
                supabaseClient.storage[BUCKET].upload(
                    path = storagePath,
                    data = fileData.bytes,
                    options = { upsert = false }
                )

                val documentMetadata = Document(
                    id = uuid4().toString(),
                    folderId = folderId,
                    name = originalFileName,
                    storageFilePath = storagePath,
                    userId = currentUserId,
                    mimeType = fileData.mimeType,
                    createdAt = Clock.System.now().toString()
                )
                supabaseClient.postgrest[DOCUMENT].insert(documentMetadata)
                _fileUploadUiState.value = FileUploadUiState.Success("File '${originalFileName}' uploaded.")
                loadDocumentsForFolder(folderId)
            } catch (e: Exception) {
                _fileUploadUiState.value = FileUploadUiState.Error("Upload failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Existing function to download to cache and then open
    fun downloadAndOpenFile(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            if (document.storageFilePath == null) {
                _fileDownloadUiState.value = FileDownloadUiState.Error("File path is missing.")
                return@launch
            }

            _fileDownloadUiState.value = FileDownloadUiState.Downloading(document.name, 0f)
            try {
                val bytes = supabaseClient.storage[BUCKET].downloadPublic(document.storageFilePath!!)
                _fileDownloadUiState.value = FileDownloadUiState.Downloading(document.name, 0.5f)

                val fileName = document.name
                val mimeType = document.mimeType ?: "application/octet-stream"

                val savedToCachePath = fileSaver.saveFile(FileData(fileName, bytes, mimeType))
                _fileDownloadUiState.value = FileDownloadUiState.Downloading(document.name, 1f)

                if (savedToCachePath != null) {
                    _fileDownloadUiState.value = FileDownloadUiState.Success(
                        fileName = fileName,
                        message = "File '$fileName' ready. Opening...",
                        localPath = savedToCachePath,
                        mimeType = mimeType
                    )
                    openFile(savedToCachePath, mimeType)
                } else {
                    _fileDownloadUiState.value = FileDownloadUiState.Error("Failed to save file '$fileName' to cache.")
                }
            } catch (e: Exception) {
                _fileDownloadUiState.value = FileDownloadUiState.Error("Download & open failed for '${document.name}': ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // New function to download to public "Downloads" folder
    fun downloadAndSaveToPublicDownloads(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            if (document.storageFilePath == null) {
                _filePublicDownloadUiState.value = FilePublicDownloadUiState.Error("File path is missing for document: ${document.name}")
                return@launch
            }

            _filePublicDownloadUiState.value = FilePublicDownloadUiState.Downloading(document.name, 0f)
            try {
                val bytes = supabaseClient.storage[BUCKET].downloadPublic(document.storageFilePath!!)
                _filePublicDownloadUiState.value = FilePublicDownloadUiState.Downloading(document.name, 0.5f) // Simulate 50% after download

                val fileData = FileData(document.name, bytes, document.mimeType ?: "application/octet-stream")
                val publicPath = fileSaver.saveFileToPublicDownloads(fileData)

                if (publicPath != null) {
                    _filePublicDownloadUiState.value = FilePublicDownloadUiState.Success(
                        fileName = document.name,
                        message = "File '${document.name}' saved to Downloads.",
                        publicPath = publicPath
                    )
                } else {
                    _filePublicDownloadUiState.value = FilePublicDownloadUiState.Error("Failed to save '${document.name}' to Downloads.")
                }
            } catch (e: Exception) {
                _filePublicDownloadUiState.value = FilePublicDownloadUiState.Error("Error saving '${document.name}' to Downloads: ${e.message}")
                e.printStackTrace()
            }
        }
    }


    fun clearFileDownloadStatus() { // For cache/open flow
        _fileDownloadUiState.value = FileDownloadUiState.Idle
    }

    fun clearFilePublicDownloadStatus() { // For public download flow
        _filePublicDownloadUiState.value = FilePublicDownloadUiState.Idle
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