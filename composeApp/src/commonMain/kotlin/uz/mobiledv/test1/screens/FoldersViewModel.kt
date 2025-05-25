// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FoldersViewModel.kt
package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
// import io.github.jan.supabase.auth.auth // Not used for custom auth user ID
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
// import io.github.jan.supabase.postgrest.query.filter.FilterOperation // Not used
// import io.github.jan.supabase.postgrest.query.filter.FilterOperator // Not used
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
import uz.mobiledv.test1.data.AuthSettings // Import AuthSettings
import uz.mobiledv.test1.di.BUCKET
import uz.mobiledv.test1.di.DOCUMENT
import uz.mobiledv.test1.di.FOLDER
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.FileData
import uz.mobiledv.test1.util.FileSaver
import com.benasher44.uuid.uuid4 // For generating UUIDs

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
    private val fileSaver: FileSaver,
    private val authSettings: AuthSettings // Inject AuthSettings
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
        loadFolders()
    }

    // Get user ID from our custom AuthSettings
    private fun getCurrentUserId(): String? {
        val id = authSettings.getCurrentUser()?.id
        if (id == null) {
            _operationStatus.value = "User not authenticated (FoldersViewModel)."
        }
        return id
    }
    private fun isCurrentUserAdmin(): Boolean {
        return authSettings.getCurrentUser()?.isAdmin == true
    }


    fun loadFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            _foldersUiState.value = FoldersUiState.Loading
            val currentUserId = getCurrentUserId()
            val isAdmin = isCurrentUserAdmin()

            if (currentUserId == null && !isAdmin) { // Non-admin must be logged in
                _foldersUiState.value = FoldersUiState.Error("User not authenticated to load folders.")
                return@launch
            }

            try {
                val query = supabaseClient.postgrest[FOLDER].select(columns = Columns.ALL) {
                    // Admin sees all folders, regular user sees only their own
                    // This assumes your RLS policies on Supabase are not restrictive for admins,
                    // or that admins need to see all folders regardless of user_id.
                    // If RLS is strict, admin might also need to query without user_id filter or use a service role.
                    // For client-side filtering if RLS is not fully covering this:
                    if (!isAdmin && currentUserId != null) {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    // If admin should only see folders they created, add:
                    // else if (isAdmin && currentUserId != null) {
                    //    filter {
                    //        eq("user_id", currentUserId) // Admin sees their own folders
                    //    }
                    // }
                    // If admin sees all folders, no user_id filter is applied here for them.
                    order("name", Order.ASCENDING)
                }
                val folders = query.decodeList<Folder>()
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
                supabaseClient.postgrest[FOLDER].insert(newFolder.toSupabaseCreateData())
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
            val isAdmin = isCurrentUserAdmin()
            try {
                supabaseClient.postgrest[FOLDER]
                    .update(
                        {
                            set("name", name)
                            set("description", description)
                            // Optionally set("updated_at", Clock.System.now().toString()) if you have such a field
                        }
                    ) {
                        filter {
                            eq("id", folderId)
                            // Ensure user can only update their own folder, unless admin
                            if (!isAdmin) {
                                eq("user_id", currentUserId)
                            }
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
            val isAdmin = isCurrentUserAdmin()
            try {
                // TODO: Add logic to delete all documents within this folder first (from DB and Storage)

                supabaseClient.postgrest[FOLDER]
                    .delete {
                        filter {
                            eq("id", folderId)
                            if (!isAdmin) { // Non-admins can only delete their own folders
                                eq("user_id", currentUserId)
                            }
                            // Admins can delete any folder (if RLS allows, or use service key for backend op)
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
        viewModelScope.launch(Dispatchers.IO) {
            _folderDocumentsUiState.value = FolderDocumentsUiState.Loading
            // User should be authenticated to see documents, or folder access is public/controlled by RLS.
            // For simplicity, we assume if they can see the folder, they can attempt to load documents.
            // RLS on the 'documents' table should enforce actual read permissions.
            val currentUserId = getCurrentUserId() // May be null if public access to folders
            val isAdmin = isCurrentUserAdmin()

            try {
                val documents = supabaseClient.postgrest[DOCUMENT]
                    .select {
                        filter {
                            eq("folder_id", folderId)
                            // Add user_id filter if documents are user-specific and not covered by RLS fully for client queries
                            // if (!isAdmin && currentUserId != null) {
                            //    eq("user_id", currentUserId)
                            // }
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
            // Only admin (desktop) can upload, based on current logic in FolderDetailScreen
            if (!isCurrentUserAdmin()) {
                _fileUploadUiState.value = FileUploadUiState.Error("Only managers can upload files.")
                return@launch
            }


            // Construct a unique path, e.g., using user ID and folder ID
            val storagePath = "user_${currentUserId}/folder_${folderId}/${uuid4()}_${fileData.name}"

            try {
                supabaseClient.storage[BUCKET].upload(
                    path = storagePath,
                    data = fileData.bytes,
                    options = { upsert = false }
                )

                val documentMetadata = Document(
                    id = uuid4().toString(), // Generate ID for document
                    folderId = folderId,
                    name = fileData.name,
                    storageFilePath = storagePath,
                    userId = currentUserId,
                    mimeType = fileData.mimeType,
                    createdAt = Clock.System.now().toString()
                )
                supabaseClient.postgrest[DOCUMENT].insert(documentMetadata.toSupabaseCreateData())

                _fileUploadUiState.value =
                    FileUploadUiState.Success("File '${fileData.name}' uploaded.")
                loadDocumentsForFolder(folderId) // Refresh list after upload
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
            // Android (non-admin) users download.
            // Ensure current user has rights to download (e.g. if it's their document or public)
            // RLS on storage bucket should handle this. For client check:
            val currentUserId = getCurrentUserId()
            if (isCurrentUserAdmin()) { // Admins on desktop might not use this direct download flow.
                _fileDownloadUiState.value = FileDownloadUiState.Error("Admin download via this button not typical.")
                return@launch
            }
            if (currentUserId == null) {
                _fileDownloadUiState.value = FileDownloadUiState.Error("User not authenticated for download.")
                return@launch
            }
            // Optional: Add check if document.userId == currentUserId if not admin and RLS isn't enough client-side hint


            _fileDownloadUiState.value = FileDownloadUiState.Loading
            try {
                // Using downloadPublic rather than authenticated as we are not relying on Supabase Auth for user context in storage RLS directly
                // Your bucket RLS policies would need to allow read access based on `postgrest` table permissions if possible, or be public.
                // If files are private and need auth context, you might need a Supabase Edge Function to proxy downloads
                // that can check custom auth session and then use service role to fetch from storage.
                // For now, assuming files are either public or RLS on storage can be configured for this custom auth.
                val bytes = supabaseClient.storage[BUCKET].downloadPublic(document.storageFilePath!!)

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
            if (!isCurrentUserAdmin()) { // Only admin can delete
                _operationStatus.value = "Only admin can delete documents."
                return@launch
            }

            if (document.id.isBlank()) { // document.id should be primary key
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
                loadDocumentsForFolder(document.folderId) // Refresh
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