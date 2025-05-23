package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.FolderRepository

sealed interface FoldersUiState {
    data object Idle : FoldersUiState
    data object Loading : FoldersUiState
    data class Success(val folders: List<Folder>) : FoldersUiState
    data class Error(val message: String) : FoldersUiState
}

class FoldersViewModel(
    private val folderRepository: FolderRepository,
    private val userRepository: uz.mobiledv.test1.repository.DocumentRepository // Assuming DocumentRepository handles current user
) : ViewModel() {

    private val _foldersUiState = MutableStateFlow<FoldersUiState>(FoldersUiState.Idle)
    val foldersUiState: StateFlow<FoldersUiState> = _foldersUiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
        loadFolders()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().fold(
                onSuccess = { _currentUser.value = it },
                onFailure = { _foldersUiState.value = FoldersUiState.Error("Failed to load user: ${it.message}") }
            )
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            _foldersUiState.value = FoldersUiState.Loading
            try {
                folderRepository.getAllFolders().fold(
                    onSuccess = { folders ->
                        _foldersUiState.value = FoldersUiState.Success(folders)
                    },
                    onFailure = { error ->
                        _foldersUiState.value = FoldersUiState.Error("Failed to load folders: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _foldersUiState.value = FoldersUiState.Error("Failed to load folders: ${e.message}")
            }
        }
    }

    fun createFolder(name: String, description: String) {
        viewModelScope.launch {
            val creatorId = _currentUser.value?.id ?: run {
                _foldersUiState.value = FoldersUiState.Error("User not logged in to create folder.")
                return@launch
            }
            folderRepository.createFolder(name, creatorId, description).fold(
                onSuccess = {
                    loadFolders() // Refresh list
                },
                onFailure = { error ->
                    // Optionally, update UI state to show specific error for creation
                    _foldersUiState.value = FoldersUiState.Error("Failed to create folder: ${error.message}")
                }
            )
        }
    }

    fun updateFolder(folderId: String, name: String, description: String) {
        viewModelScope.launch {
            folderRepository.updateFolder(folderId, name, description).fold(
                onSuccess = {
                    loadFolders() // Refresh list
                },
                onFailure = { error ->
                    _foldersUiState.value = FoldersUiState.Error("Failed to update folder: ${error.message}")
                }
            )
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId).fold(
                onSuccess = {
                    loadFolders() // Refresh list
                },
                onFailure = { error ->
                    _foldersUiState.value = FoldersUiState.Error("Failed to delete folder: ${error.message}")
                }
            )
        }
    }

    fun resetState() {
        _foldersUiState.value = FoldersUiState.Idle
    }
}