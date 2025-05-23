package uz.mobiledv.test1.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uz.mobiledv.test1.model.User // Assuming User is in this package

interface AuthSettings {
    fun saveLastLoggedInEmail(email: String?)
    fun getLastLoggedInEmail(): String?
    fun saveLoginStatus(isLoggedIn: Boolean)
    fun getLoginStatus(): Boolean
    fun saveUser(user: User?) // New
    fun getUser(): User?      // New
    fun clearAuthSettings()
}

internal const val KEY_LAST_LOGGED_IN_EMAIL = "last_logged_in_email"
internal const val KEY_IS_LOGGED_IN = "is_logged_in"
internal const val KEY_USER_DATA = "user_data" // New key

class AuthSettingsImpl(private val settings: Settings, private val json: Json) : AuthSettings {

    override fun saveLastLoggedInEmail(email: String?) {
        if (email != null) {
            settings.putString(KEY_LAST_LOGGED_IN_EMAIL, email)
        } else {
            settings.remove(KEY_LAST_LOGGED_IN_EMAIL)
        }
    }

    override fun getLastLoggedInEmail(): String? {
        return settings.getStringOrNull(KEY_LAST_LOGGED_IN_EMAIL)
    }

    override fun saveLoginStatus(isLoggedIn: Boolean) {
        settings.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
    }

    override fun getLoginStatus(): Boolean {
        return settings.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    override fun saveUser(user: User?) { // New
        if (user != null) {
            try {
                val userJson = json.encodeToString(user)
                settings.putString(KEY_USER_DATA, userJson)
            } catch (e: Exception) {
                // Handle serialization error, e.g., log it
                println("Error saving user to settings: ${e.message}")
                settings.remove(KEY_USER_DATA) // Clear if serialization fails
            }
        } else {
            settings.remove(KEY_USER_DATA)
        }
    }

    override fun getUser(): User? { // New
        val userJson = settings.getStringOrNull(KEY_USER_DATA)
        return if (userJson != null) {
            try {
                json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                // Handle deserialization error, e.g., log it
                println("Error reading user from settings: ${e.message}")
                null // Return null if deserialization fails
            }
        } else {
            null
        }
    }

    override fun clearAuthSettings() {
        settings.remove(KEY_LAST_LOGGED_IN_EMAIL)
        settings.remove(KEY_IS_LOGGED_IN)
        settings.remove(KEY_USER_DATA) // Clear user data
    }
}