package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.di.BUCKET
import uz.mobiledv.test1.di.DOCUMENT
import uz.mobiledv.test1.di.FOLDER
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.DirectoryUploadRequest
import uz.mobiledv.test1.util.FileData
import uz.mobiledv.test1.util.FileSaver
import uz.mobiledv.test1.util.openFile
import java.util.Objects.isNull

// --- UI States ---
sealed class FolderContentUiState {
    data object Idle : FolderContentUiState()
    data object Loading : FolderContentUiState()
    data class Success(val subFolders: List<Folder>, val documents: List<Document>) :
        FolderContentUiState()

    data class Error(val message: String) : FolderContentUiState()
}

sealed class DirectoryUploadUiState {
    data object Idle : DirectoryUploadUiState()
    data class Uploading(val message: String, val progress: Float) : DirectoryUploadUiState() // progress can be overall
    data class Success(val message: String) : DirectoryUploadUiState()
    data class Error(val message: String) : DirectoryUploadUiState()
}

sealed class FileUploadUiState {
    data object Idle : FileUploadUiState()
    data object Loading : FileUploadUiState() // Represents loading for the entire batch
    data class Success(val message: String) : FileUploadUiState() // Overall success/summary message
    data class Error(val message: String) : FileUploadUiState() // Overall error/summary message
}

sealed class FileDownloadUiState { // For cache/open
    data object Idle : FileDownloadUiState()
    data object Loading : FileDownloadUiState()
    data class Downloading(val fileName: String, val progress: Float) : FileDownloadUiState()
    data class Success(
        val fileName: String,
        val message: String,
        val localPath: String,
        val mimeType: String?
    ) : FileDownloadUiState()

    data class Error(val message: String) : FileDownloadUiState()
}

sealed class FilePublicDownloadUiState { // For public "Downloads"
    data object Idle : FilePublicDownloadUiState()
    data class Downloading(val fileName: String, val progress: Float) : FilePublicDownloadUiState()
    data class Success(val fileName: String, val message: String, val publicPath: String?) :
        FilePublicDownloadUiState()

    data class Error(val message: String) : FilePublicDownloadUiState()
}

class FoldersViewModel(
    private val supabaseClient: SupabaseClient,
    private val fileSaver: FileSaver,
    private val authSettings: AuthSettings
) : ViewModel() {

    private val _folderContentUiState =
        MutableStateFlow<FolderContentUiState>(FolderContentUiState.Idle)
    val folderContentUiState: StateFlow<FolderContentUiState> = _folderContentUiState.asStateFlow()

    private val _fileUploadUiState = MutableStateFlow<FileUploadUiState>(FileUploadUiState.Idle)
    val fileUploadUiState: StateFlow<FileUploadUiState> = _fileUploadUiState.asStateFlow()

    private val _operationStatus =
        MutableStateFlow<String?>(null) // For general messages (create, update, delete folder)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val _fileDownloadUiState =
        MutableStateFlow<FileDownloadUiState>(FileDownloadUiState.Idle)
    val fileDownloadUiState: StateFlow<FileDownloadUiState> = _fileDownloadUiState.asStateFlow()

    private val _filePublicDownloadUiState =
        MutableStateFlow<FilePublicDownloadUiState>(FilePublicDownloadUiState.Idle)
    val filePublicDownloadUiState: StateFlow<FilePublicDownloadUiState> =
        _filePublicDownloadUiState.asStateFlow()

    private val _directoryUploadUiState = MutableStateFlow<DirectoryUploadUiState>(DirectoryUploadUiState.Idle)
    val directoryUploadUiState: StateFlow<DirectoryUploadUiState> = _directoryUploadUiState.asStateFlow()


    private fun getCurrentUserId(): String? = authSettings.getCurrentUser()?.id
    private fun isCurrentUserAdmin(): Boolean = authSettings.getCurrentUser()?.isAdmin == true

// In FoldersViewModel.kt

    fun loadFolderContents(parentId: String?) { // parentId is the ID of the folder whose contents we want to load
        viewModelScope.launch(Dispatchers.IO) {
            _folderContentUiState.value = FolderContentUiState.Loading
            val currentUserId = getCurrentUserId() ?: run {
                _folderContentUiState.value = FolderContentUiState.Error("User not authenticated.")
                return@launch
            }

            try {
                // Fetch subfolders for the given parentId
                val subFolders = supabaseClient.postgrest[FOLDER].select {
                    filter {
                        if (parentId == null) {
                            filter("parent_id", FilterOperator.IS,"null")
                        } else {
                            eq("parent_id", parentId)
                        }
                    }
                    order("name", Order.ASCENDING)
                }.decodeList<Folder>()

                val documents = if (parentId != null) {
                    supabaseClient.postgrest[DOCUMENT].select {
                        filter {
                            eq("folder_id", parentId)
                        }
                        order("name", Order.ASCENDING)
                    }.decodeList<Document>()
                } else {
                    emptyList()
                }

                _folderContentUiState.value = FolderContentUiState.Success(subFolders, documents)

            } catch (e: Exception) {
                _folderContentUiState.value =
                    FolderContentUiState.Error("Error loading contents: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun createFolder(name: String, description: String, parentId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = getCurrentUserId() ?: run {
                _operationStatus.value = "User not authenticated to create folder."
                return@launch
            }
            try {
                val newFolder = Folder(
                    id = uuid4().toString(),
                    name = name.trim(),
                    description = description.trim(),
                    userId = currentUserId,
                    parentId = parentId,
                    createdAt = Clock.System.now().toString()
                )
                supabaseClient.postgrest[FOLDER].insert(newFolder)
                _operationStatus.value = "Folder '$name' created successfully."
                loadFolderContents(parentId)
            } catch (e: Exception) {
                _operationStatus.value = "Error creating folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun updateFolder(
        folderId: String,
        name: String,
        description: String,
        currentParentId: String?
    ) {
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
                            set("name", name.trim())
                            set("description", description.trim())
                        }
                    ) {
                        filter { eq("id", folderId) }
                    }
                _operationStatus.value = "Folder '$name' updated successfully."
                loadFolderContents(currentParentId)
            } catch (e: Exception) {
                _operationStatus.value = "Error updating folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun deleteFolder(folderId: String, parentIdOfDeletedFolder: String?) {
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
                supabaseClient.postgrest[FOLDER].delete { filter { eq("id", folderId) } }
                _operationStatus.value =
                    "Folder deleted. (Note: Contents might require manual cleanup or DB cascade rules)."
                loadFolderContents(parentIdOfDeletedFolder)
            } catch (e: Exception) {
                _operationStatus.value = "Error deleting folder: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun uploadFilesToFolder(targetFolderId: String, filesData: List<FileData>) {
        viewModelScope.launch(Dispatchers.IO) {
            _fileUploadUiState.value = FileUploadUiState.Loading
            val currentUserId = getCurrentUserId() ?: run {
                _fileUploadUiState.value = FileUploadUiState.Error("User not authenticated.")
                return@launch
            }
            if (!isCurrentUserAdmin()) {
                _fileUploadUiState.value =
                    FileUploadUiState.Error("Only designated managers can upload files.")
                return@launch
            }

            var successfulUploads = 0
            var failedUploads = 0
            val totalFiles = filesData.size

            for (fileData in filesData) {
                val originalFileName = fileData.name
                val fileExtension = originalFileName.substringAfterLast('.', "").trim().lowercase()
                val safeExtension =
                    if (fileExtension.matches(Regex("^[a-zA-Z0-9]+$")) && fileExtension.length <= 5) {
                        ".$fileExtension"
                    } else {
                        ""
                    }
                val uniqueStorageObjectName = "${uuid4()}$safeExtension"
                val storagePath =
                    "user_${currentUserId}/folder_${targetFolderId}/${uniqueStorageObjectName}"

                try {
                    supabaseClient.storage[BUCKET].upload(
                        path = storagePath,
                        data = fileData.bytes,
                        options = { upsert = false }
                    )

                    val documentMetadata = Document(
                        id = uuid4().toString(),
                        folderId = targetFolderId,
                        name = originalFileName,
                        storageFilePath = storagePath,
                        userId = currentUserId,
                        mimeType = fileData.mimeType,
                        createdAt = Clock.System.now().toString()
                    )
                    supabaseClient.postgrest[DOCUMENT].insert(documentMetadata)
                    successfulUploads++
                } catch (e: Exception) {
                    failedUploads++
                    println("Upload failed for ${fileData.name}: ${e.message}") // Log individual error
                    e.printStackTrace()
                }
            }

            val message = when {
                successfulUploads == totalFiles && totalFiles > 0 -> "$successfulUploads file(s) uploaded successfully."
                successfulUploads > 0 -> "$successfulUploads file(s) uploaded. $failedUploads failed."
                failedUploads > 0 -> "All $failedUploads file upload(s) failed."
                else -> "No files were processed." // Should not happen if filesData is not empty
            }

            if (failedUploads > 0 && successfulUploads == 0) {
                _fileUploadUiState.value = FileUploadUiState.Error(message)
            } else {
                _fileUploadUiState.value = FileUploadUiState.Success(message)
            }
            if (successfulUploads > 0) {
                loadFolderContents(targetFolderId)
            }
        }
    }


    fun downloadAndOpenFile(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            if (document.storageFilePath == null) {
                _fileDownloadUiState.value = FileDownloadUiState.Error("File path is missing.")
                return@launch
            }
            _fileDownloadUiState.value = FileDownloadUiState.Downloading(document.name, 0f)
            try {
                val bytes =
                    supabaseClient.storage[BUCKET].downloadPublic(document.storageFilePath!!)
                _fileDownloadUiState.value = FileDownloadUiState.Downloading(document.name, 0.5f)

                val fileDataInstance =
                    FileData(document.name, bytes, document.mimeType ?: "application/octet-stream")
                val savedToCachePath = fileSaver.saveFile(fileDataInstance)
                _fileDownloadUiState.value = FileDownloadUiState.Downloading(document.name, 1f)

                if (savedToCachePath != null) {
                    _fileDownloadUiState.value = FileDownloadUiState.Success(
                        fileName = document.name,
                        message = "File '${document.name}' ready. Opening...",
                        localPath = savedToCachePath,
                        mimeType = document.mimeType
                    )
                    openFile(savedToCachePath, document.mimeType)
                } else {
                    _fileDownloadUiState.value =
                        FileDownloadUiState.Error("Failed to save file '${document.name}' to cache.")
                }
            } catch (e: Exception) {
                _fileDownloadUiState.value =
                    FileDownloadUiState.Error("Download & open failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun downloadAndSaveToPublicDownloads(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            if (document.storageFilePath == null) {
                _filePublicDownloadUiState.value =
                    FilePublicDownloadUiState.Error("File path is missing.")
                return@launch
            }
            _filePublicDownloadUiState.value =
                FilePublicDownloadUiState.Downloading(document.name, 0f)
            try {
                val bytes =
                    supabaseClient.storage[BUCKET].downloadPublic(document.storageFilePath!!)
                _filePublicDownloadUiState.value =
                    FilePublicDownloadUiState.Downloading(document.name, 0.5f)

                val fileDataInstance =
                    FileData(document.name, bytes, document.mimeType ?: "application/octet-stream")
                val publicPath = fileSaver.saveFileToPublicDownloads(fileDataInstance)

                if (publicPath != null) {
                    _filePublicDownloadUiState.value = FilePublicDownloadUiState.Success(
                        fileName = document.name,
                        message = "File '${document.name}' saved to Downloads.",
                        publicPath = publicPath
                    )
                } else {
                    _filePublicDownloadUiState.value =
                        FilePublicDownloadUiState.Error("Failed to save '${document.name}'.")
                }
            } catch (e: Exception) {
                _filePublicDownloadUiState.value =
                    FilePublicDownloadUiState.Error("Download to public failed: ${e.message}")
                e.printStackTrace()
            }
        }
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
                loadFolderContents(document.folderId)
            } catch (e: Exception) {
                _operationStatus.value = "Error deleting document '${document.name}': ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun uploadDirectory(currentSupabaseParentFolderId: String?, request: DirectoryUploadRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            _directoryUploadUiState.value = DirectoryUploadUiState.Uploading("Starting upload for folder '${request.directoryName}'...", 0f)
            val currentUserId = getCurrentUserId() ?: run {
                _directoryUploadUiState.value = DirectoryUploadUiState.Error("User not authenticated.")
                return@launch
            }
            // Add permission check if needed, e.g., based on appViewModel.isManager
            // if (!isCurrentUserAdmin()) {
            //     _directoryUploadUiState.value = DirectoryUploadUiState.Error("User does not have permission.")
            //     return@launch
            // }

            try {
                val createdRootFolderId = processAndUploadDirectory(currentSupabaseParentFolderId, request, currentUserId, 0f, 1f)
                _directoryUploadUiState.value = DirectoryUploadUiState.Success("Folder '${request.directoryName}' and its contents uploaded successfully.")
                loadFolderContents(currentSupabaseParentFolderId) // Refresh the list where the new folder was added
            } catch (e: Exception) {
                _directoryUploadUiState.value = DirectoryUploadUiState.Error("Failed to upload folder structure: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun processAndUploadDirectory(
        parentSupabaseFolderId: String?, // ID of the Supabase folder where this directory should be created
        dirRequest: DirectoryUploadRequest,
        userId: String,
        progressStart: Float, // For progress tracking
        progressRange: Float  // For progress tracking
    ): String { // Returns the ID of the created Supabase folder

        val currentDirProgressPortion = 0.2f * progressRange // 20% for creating this dir and its direct files

        // 1. Create the current directory in Supabase
        _directoryUploadUiState.value = DirectoryUploadUiState.Uploading(
            "Creating folder: ${dirRequest.directoryName}",
            progressStart
        )
        val newSupabaseFolder = Folder(
            id = uuid4().toString(), // Generate new UUID for the folder
            name = dirRequest.directoryName,
            description = "Uploaded folder: ${dirRequest.directoryName}",
            userId = userId,
            parentId = parentSupabaseFolderId,
            createdAt = Clock.System.now().toString()
        )
        supabaseClient.postgrest[FOLDER].insert(newSupabaseFolder)
        // _operationStatus.value = "Folder '${newSupabaseFolder.name}' created." // Optional finer-grained status

        val newFolderSupabaseId = newSupabaseFolder.id

        // 2. Upload files directly within the current directory
        if (dirRequest.files.isNotEmpty()) {
            _directoryUploadUiState.value = DirectoryUploadUiState.Uploading(
                "Uploading ${dirRequest.files.size} files in ${dirRequest.directoryName}...",
                progressStart + (currentDirProgressPortion * 0.1f) // Increment progress slightly
            )
            // Re-use or adapt your existing batch file upload logic.
            // Simplified version for demonstration:
            for ((index, fileData) in dirRequest.files.withIndex()) {
                val fileProgress = (index.toFloat() / dirRequest.files.size) * (currentDirProgressPortion * 0.9f)
                _directoryUploadUiState.value = DirectoryUploadUiState.Uploading(
                    "Uploading file ${fileData.name} in ${dirRequest.directoryName}...",
                    progressStart + (currentDirProgressPortion * 0.1f) + fileProgress
                )
                val originalFileName = fileData.name
                // Ensure unique storage path, e.g., using UUID
                val fileExtension = originalFileName.substringAfterLast('.', "").let { if (it.isNotEmpty()) ".$it" else "" }
                val storageFileName = "${uuid4()}$fileExtension"
                val storagePath = "user_${userId}/folder_${newFolderSupabaseId}/${storageFileName}"

                supabaseClient.storage[BUCKET].upload(
                    path = storagePath,
                    data = fileData.bytes,
                    options = { upsert = false } // Or true if you want to overwrite
                )

                val documentMetadata = Document(
                    id = uuid4().toString(),
                    folderId = newFolderSupabaseId,
                    name = originalFileName,
                    storageFilePath = storagePath,
                    userId = userId,
                    mimeType = fileData.mimeType,
                    createdAt = Clock.System.now().toString()
                )
                supabaseClient.postgrest[DOCUMENT].insert(documentMetadata)
            }
            // _operationStatus.value = "${dirRequest.files.size} files uploaded to '${dirRequest.directoryName}'."
        }

        // 3. Recursively process subdirectories
        val subDirectoriesProgressPortion = 0.8f * progressRange // Remaining 80% for subfolders
        var currentSubDirProgress = progressStart + currentDirProgressPortion

        if (dirRequest.subDirectories.isNotEmpty()) {
            val progressPerSubDir = subDirectoriesProgressPortion / dirRequest.subDirectories.size
            for (subDirRequest in dirRequest.subDirectories) {
                processAndUploadDirectory(newFolderSupabaseId, subDirRequest, userId, currentSubDirProgress, progressPerSubDir)
                currentSubDirProgress += progressPerSubDir
            }
        }
        return newFolderSupabaseId
    }

    fun clearDirectoryUploadStatus() {
        _directoryUploadUiState.value = DirectoryUploadUiState.Idle
    }

    // --- Clear Status Functions ---
    fun clearFileUploadStatus() {
        _fileUploadUiState.value = FileUploadUiState.Idle
    }

    fun clearFileDownloadStatus() {
        _fileDownloadUiState.value = FileDownloadUiState.Idle
    }

    fun clearFilePublicDownloadStatus() {
        _filePublicDownloadUiState.value = FilePublicDownloadUiState.Idle
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}