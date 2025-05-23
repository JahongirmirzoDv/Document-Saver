package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.model.UserPrefs
import uz.mobiledv.test1.repository.DocumentRepository // Assuming DocumentRepository handles user ops

sealed interface UserManagementUiState {
    data object Idle : UserManagementUiState
    data object Loading : UserManagementUiState
    data class Success(val users: List<User>) : UserManagementUiState
    data class Error(val message: String) : UserManagementUiState
}

class UserManagementViewModel(
    private val userRepository: DocumentRepository // Using DocumentRepository for user operations
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserManagementUiState>(UserManagementUiState.Idle)
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UserManagementUiState.Loading
            userRepository.getUsers() // This is a Flow
                .onEach { users ->
                    _uiState.value = UserManagementUiState.Success(users)
                }
                .launchIn(viewModelScope) // Collect the flow
        }
    }

    fun createUser(username: String, email: String, password: String, isAdmin: Boolean) {
        viewModelScope.launch {
            val user = User(
                username = username,
                email = email,
                password = password,
                isAdmin = isAdmin,
                prefs = UserPrefs(isAdmin = isAdmin)
            )
            userRepository.createUser(user).fold(
                onSuccess = { loadUsers() }, // Refresh list
                onFailure = { _uiState.value = UserManagementUiState.Error("Failed to create user: ${it.message}") }
            )
        }
    }

    fun updateUser(userId: String, username: String, email: String?, isAdmin: Boolean) {
        viewModelScope.launch {
            // First update basic info if provided
            if (username.isNotBlank() || email != null) {
                 userRepository.updateUser(userId, username.ifBlank { null }, email, null).fold(
                     onSuccess = {}, // Prefs will be updated next
                     onFailure = {
                         _uiState.value = UserManagementUiState.Error("Failed to update user details: ${it.message}")
                         return@launch
                     }
                 )
            }

            // Then update preferences (like isAdmin)
            val prefs = UserPrefs(isAdmin = isAdmin)
            userRepository.updateUserPrefs(userId, prefs).fold(
                onSuccess = { loadUsers() }, // Refresh list after all updates
                onFailure = { _uiState.value = UserManagementUiState.Error("Failed to update user preferences: ${it.message}") }
            )
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            userRepository.deleteUser(userId).fold(
                onSuccess = { loadUsers() }, // Refresh list
                onFailure = { _uiState.value = UserManagementUiState.Error("Failed to delete user: ${it.message}") }
            )
        }
    }
     fun resetState() {
        _uiState.value = UserManagementUiState.Idle
    }
}