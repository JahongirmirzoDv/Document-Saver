package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.DocumentRepository

sealed interface DocumentsUiState {
    data object Idle : DocumentsUiState
    data object Loading : DocumentsUiState
    data class Success(val documents: List<Document>) : DocumentsUiState
    data class Error(val message: String) : DocumentsUiState
}

class DocumentsViewModel(
    private val documentRepository: DocumentRepository,
    private val userRepository: uz.mobiledv.test1.repository.DocumentRepository // For current user
) : ViewModel() {

    private val _documentsUiState = MutableStateFlow<DocumentsUiState>(DocumentsUiState.Idle)
    val documentsUiState: StateFlow<DocumentsUiState> = _documentsUiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

     private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().fold(
                onSuccess = { _currentUser.value = it },
                onFailure = { _documentsUiState.value = DocumentsUiState.Error("Failed to load user: ${it.message}") }
            )
        }
    }


    fun loadDocuments(folderId: String) {
        viewModelScope.launch {
            _documentsUiState.value = DocumentsUiState.Loading
            documentRepository.getDocumentsByFolder(folderId).fold(
                onSuccess = { documents ->
                    _documentsUiState.value = DocumentsUiState.Success(documents)
                },
                onFailure = { error ->
                    _documentsUiState.value = DocumentsUiState.Error("Failed to load documents: ${error.message}")
                }
            )
        }
    }

    fun createDocument(folderId: String, name: String, content: String, fileBytes: ByteArray?, fileName: String?, mimeType: String?) {
        viewModelScope.launch {
            val creatorId = _currentUser.value?.id ?: run {
                _documentsUiState.value = DocumentsUiState.Error("User not logged in.")
                return@launch
            }
            val documentMetadata = Document(
                folderId = folderId,
                name = name,
                content = content, // For text-based or as initial description
                createdBy = creatorId
            )

            if (fileBytes != null && fileName != null) {
                // Upload with file
                documentRepository.uploadDocument(documentMetadata, fileBytes, fileName, mimeType).fold(
                    onSuccess = { loadDocuments(folderId) },
                    onFailure = { _documentsUiState.value = DocumentsUiState.Error("Failed to upload document: ${it.message}") }
                )
            } else {
                // Create metadata only
                documentRepository.createDocumentMetadata(documentMetadata).fold(
                    onSuccess = { loadDocuments(folderId) },
                    onFailure = { _documentsUiState.value = DocumentsUiState.Error("Failed to create document metadata: ${it.message}") }
                )
            }
        }
    }

    fun updateDocument(documentId: String, folderId: String, name: String, content: String) {
         viewModelScope.launch {
            documentRepository.updateTextDocumentContent(documentId, name, content).fold(
                onSuccess = { loadDocuments(folderId) },
                onFailure = { _documentsUiState.value = DocumentsUiState.Error("Failed to update document: ${it.message}") }
            )
        }
    }

    fun deleteDocument(documentId: String, appwriteFileId: String?, folderId: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId, appwriteFileId).fold(
                onSuccess = { loadDocuments(folderId) },
                onFailure = { _documentsUiState.value = DocumentsUiState.Error("Failed to delete document: ${it.message}") }
            )
        }
    }

    fun resetState() {
        _documentsUiState.value = DocumentsUiState.Idle
    }
}