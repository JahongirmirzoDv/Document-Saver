package uz.mobiledv.test1


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.mfa.MfaFactor
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class AppViewModel(
    val supabaseClient: SupabaseClient
) : ViewModel() {

    val sessionStatus = supabaseClient.auth.sessionStatus
    val loginAlert = MutableStateFlow<String?>(null)
    val statusFlow = supabaseClient.auth.mfa.statusFlow
    val enrolledFactor = MutableStateFlow<MfaFactor<FactorType.TOTP.Response>?>(null)

    //Auth

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            kotlin.runCatching {
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onSuccess {
                loginAlert.value = "Successfully registered! Check your E-Mail to verify your account."
            }.onFailure {
                loginAlert.value = "There was an error while registering: ${it.message}"
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            kotlin.runCatching {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onFailure {
                it.printStackTrace()
                loginAlert.value = "There was an error while logging in. Check your credentials and try again."
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