package uz.mobiledv.test1


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.mfa.MfaFactor
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import uz.mobiledv.test1.util.PlatformType
import uz.mobiledv.test1.util.getCurrentPlatform


class AppViewModel(
    val supabaseClient: SupabaseClient
) : ViewModel() {

    val currentPlatform: PlatformType = getCurrentPlatform()
    val isManager: Boolean = currentPlatform == PlatformType.DESKTOP

    val sessionStatus = supabaseClient.auth.sessionStatus
    val loginAlert = MutableStateFlow<String?>(null)
    val statusFlow = supabaseClient.auth.mfa.statusFlow
    val enrolledFactor = MutableStateFlow<MfaFactor<FactorType.TOTP.Response>?>(null)

    //Auth

    fun signUp(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onSuccess {
                loginAlert.value =
                    "Successfully registered! Check your E-Mail to verify your account."
            }.onFailure {
                loginAlert.value = "There was an error while registering: ${it.message}"
            }
        }
    }

    fun adminCreateUser(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                // Using the standard signUpWith for simplicity and to enforce email verification.
                // If you have Supabase admin privileges and want to bypass email verification
                // or set email_confirmed_at = true, you might explore admin-specific APIs
                // (e.g., via a service role key on a backend or a secure Cloud Function).
                // For a client-side desktop app, using the standard flow is often more secure.
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    // You could potentially add user_metadata here if needed, e.g.,
                    // data = buildJsonObject { put("created_by_admin", true) }
                }
            }.onSuccess {
                loginAlert.value =
                    "User account created for $email. They need to check their email to verify the account."
            }.onFailure {
                loginAlert.value = "Error creating user account: ${it.message}"
            }
        }
    }

    fun login(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

            }.onFailure {
                it.printStackTrace()
                loginAlert.value =
                    "There was an error while logging in. Check your credentials and try again."
            }
        }
    }

    fun logout() {
        enrolledFactor.value = null
        viewModelScope.launch {
            kotlin.runCatching {
                supabaseClient.auth.signOut()
            }
        }
    }
}