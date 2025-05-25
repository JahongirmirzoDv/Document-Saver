// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/AppViewModel.kt
package uz.mobiledv.test1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.di.USERS_TABLE
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.util.PlatformType
import uz.mobiledv.test1.util.getCurrentPlatform

// WARNING: PLAIN TEXT PASSWORD STORAGE AND CHECKING - EXTREMELY INSECURE
fun storePlainTextPassword(password: String): String {
    println("WARNING: Storing plain text password. This is EXTREMELY INSECURE.")
    return password // Store the password as is
}

fun checkPlainTextPassword(plainPasswordFromInput: String, plainPasswordFromDb: String): Boolean {
    println("WARNING: Comparing plain text passwords. This is EXTREMELY INSECURE.")
    return plainPasswordFromInput == plainPasswordFromDb // Direct comparison
}


// Custom Session Status
sealed class CustomSessionStatus {
    data object Initializing : CustomSessionStatus()
    data class Authenticated(val user: User) : CustomSessionStatus()
    data object NotAuthenticated : CustomSessionStatus()
}

class AppViewModel(
    private val supabaseClient: SupabaseClient,
    private val authSettings: AuthSettings
) : ViewModel() {

    val currentPlatform: PlatformType = getCurrentPlatform()

    private val _customSessionStatus = MutableStateFlow<CustomSessionStatus>(CustomSessionStatus.Initializing)
    val customSessionStatus: StateFlow<CustomSessionStatus> = _customSessionStatus.asStateFlow()

    val operationAlert = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val storedUser = authSettings.getCurrentUser()
            if (storedUser != null) {
                _customSessionStatus.value = CustomSessionStatus.Authenticated(storedUser)
            } else {
                _customSessionStatus.value = CustomSessionStatus.NotAuthenticated
            }
        }
    }

    // isManager defines if the user has administrative capabilities (typically on Desktop)
    // This primarily controls UI elements for actions like creating users, folders, etc.
    // Read access to data is now intended to be universal for all authenticated users.
    val isManager: Boolean
        get() = (customSessionStatus.value as? CustomSessionStatus.Authenticated)?.user?.isAdmin == true && currentPlatform == PlatformType.DESKTOP


    fun login(identifier: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _customSessionStatus.value = CustomSessionStatus.Initializing
            operationAlert.value = null
            try {
                val userList = supabaseClient.postgrest[USERS_TABLE].select {
                    filter {
                        or {
                            eq("username", identifier)
                            eq("email", identifier)
                        }
                    }
                    limit(1)
                }.decodeList<User>()

                if (userList.isNotEmpty()) {
                    val user = userList.first()
                    // Assuming passwordValue is the field in User data class
                    if (user.passwordHash != null && checkPlainTextPassword(password, user.passwordHash!!)) {
                        authSettings.saveCurrentUser(user)
                        if (rememberMe && user.email != null) {
                            authSettings.saveLastLoggedInEmail(user.email)
                        } else if (!rememberMe) {
                            authSettings.saveLastLoggedInEmail(null)
                        }
                        _customSessionStatus.value = CustomSessionStatus.Authenticated(user)
                        operationAlert.value = "Login successful!"
                    } else {
                        authSettings.clearAuthSettings()
                        _customSessionStatus.value = CustomSessionStatus.NotAuthenticated
                        operationAlert.value = "Invalid username/email or password."
                    }
                } else {
                    authSettings.clearAuthSettings()
                    _customSessionStatus.value = CustomSessionStatus.NotAuthenticated
                    operationAlert.value = "User not found."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                authSettings.clearAuthSettings()
                _customSessionStatus.value = CustomSessionStatus.NotAuthenticated
                operationAlert.value = "Login error: ${e.message}"
            }
        }
    }

    fun adminCreateUser(username: String, email: String, password: String, isAdmin: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            operationAlert.value = null
            // User creation is an administrative action, controlled by isManager
            if (!isManager) {
                operationAlert.value = "Only designated managers (Desktop admin) can create users."
                return@launch
            }

            try {
                val existingUserCount = supabaseClient.postgrest[USERS_TABLE].select(columns = Columns.ALL) {
                    filter {
                        or {
                            eq("username", username)
                            eq("email", email)
                        }
                    }
                }.countOrNull()

                if (existingUserCount != null && existingUserCount > 0) {
                    operationAlert.value = "Username or email already exists."
                    return@launch
                }

                val plainTextPasswordForDb = storePlainTextPassword(password)

                val newUser = User(
                    id = uuid4().toString(),
                    username = username,
                    email = email,
                    passwordHash = plainTextPasswordForDb,
                    isAdmin = isAdmin, // Admin flag set by the creator
                    createdAt = Clock.System.now().toString()
                )

                supabaseClient.postgrest[USERS_TABLE].insert(newUser)
                operationAlert.value = "User '$username' created successfully."

            } catch (e: Exception) {
                operationAlert.value = "Error creating user: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authSettings.clearAuthSettings()
            _customSessionStatus.value = CustomSessionStatus.NotAuthenticated
            operationAlert.value = "Logged out."
        }
    }

    fun getCurrentUserId(): String? {
        return (customSessionStatus.value as? CustomSessionStatus.Authenticated)?.user?.id
    }

    fun getCurrentUser(): User? {
        return (customSessionStatus.value as? CustomSessionStatus.Authenticated)?.user
    }
}