package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.DocumentRepository

class LoginViewModel(
    private val documentRepository: DocumentRepository // Injected by Koin
) : ViewModel() { // Or your KMP ViewModel base class

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading
            val result = documentRepository.login(email, password) // Using the injected repository
            result.fold(
                onSuccess = { user ->
                    _loggedInUser.value = user
                    _loginUiState.value = LoginUiState.Success(user)
                },
                onFailure = { error ->
                    _loginUiState.value = LoginUiState.Error(error.message ?: "Unknown login error")
                }
            )
        }
    }

    fun resetState() {
        _loginUiState.value = LoginUiState.Idle
    }
}

// Sealed interface to represent UI states
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val message: String) : LoginUiState
}