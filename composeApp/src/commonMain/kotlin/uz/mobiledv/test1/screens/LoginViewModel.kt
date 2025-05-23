package uz.mobiledv.test1.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.appwrite.services.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.mobiledv.test1.data.AuthSettings // <-- Import
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.DocumentRepository

class LoginViewModel(
    private val documentRepository: DocumentRepository,
    private val authSettings: AuthSettings // <-- Inject
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    // To pre-fill email on the login screen
    val lastLoggedInEmail: String?
        get() = authSettings.getLastLoggedInEmail()

    // To check initial login status for auto-navigation
    val initialLoginStatus: Boolean
        get() = authSettings.getLoginStatus()


    fun login(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginUiState.value = LoginUiState.Loading
            val result = documentRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _loggedInUser.value = user
                    _loginUiState.value = LoginUiState.Success(user)
                    authSettings.saveLoginStatus(true)
                    if (rememberMe) {
                        authSettings.saveLastLoggedInEmail(email)
                    } else {
                        authSettings.saveLastLoggedInEmail(null) // Clear if not remembered
                    }
                },
                onFailure = { error ->
                    _loginUiState.value = LoginUiState.Error(error.message ?: "Unknown login error")
                    authSettings.saveLoginStatus(false) // Ensure status is false on failed login
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            // Call Appwrite logout
            documentRepository.logout().fold(
                onSuccess = {
                    authSettings.clearAuthSettings()
                    _loggedInUser.value = null
                    // You might want a specific UI state for logout completion
                    _loginUiState.value = LoginUiState.Idle // Or a new LogoutSuccess state
                },
                onFailure = {
                    // Handle logout error, though typically you'd still clear local settings
                    authSettings.clearAuthSettings() // Clear local anyway
                    _loggedInUser.value = null
                    _loginUiState.value = LoginUiState.Error("Logout failed: ${it.message}")
                }
            )
        }
    }

    // Call this on app start to check if the Appwrite session is still valid
    fun checkActiveSessionAndNavigate(): Boolean {
        if (authSettings.getLoginStatus()) { // Only proceed if we think user was logged in
            viewModelScope.launch(Dispatchers.IO) {
                documentRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        if (user != null) {
                            _loggedInUser.value = user
                            _loginUiState.value = LoginUiState.Success(user) // Triggers navigation
                        } else {
                            // No active Appwrite session, despite local flag
                            authSettings.saveLoginStatus(false) // Correct the local flag
                            _loginUiState.value = LoginUiState.Idle // Stay on login
                        }
                    },
                    onFailure = {
                        // Error checking session, treat as not logged in
                        authSettings.saveLoginStatus(false)
                        _loginUiState.value = LoginUiState.Idle
                    }
                )
            }
            return true // Indicates an attempt to check session is made
        }
        return false // No local logged-in flag, stay on login
    }


    fun resetState() {
        _loginUiState.value = LoginUiState.Idle
    }
}

// Sealed interface to represent UI states (no changes needed here for now)
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val message: String) : LoginUiState
}