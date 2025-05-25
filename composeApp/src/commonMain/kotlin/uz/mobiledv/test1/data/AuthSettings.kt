// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/data/AuthSettings.kt
package uz.mobiledv.test1.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uz.mobiledv.test1.model.User

interface AuthSettings {
    fun saveLastLoggedInEmail(email: String?) // Keep for convenience if desired
    fun getLastLoggedInEmail(): String?    // Keep for convenience if desired

    fun saveCurrentUser(user: User?)
    fun getCurrentUser(): User?
    fun clearAuthSettings()
}

internal const val KEY_LAST_LOGGED_IN_EMAIL = "last_logged_in_email"
internal const val KEY_CURRENT_USER_DATA = "current_user_data" // Changed from KEY_USER_DATA

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

    override fun saveCurrentUser(user: User?) {
        if (user != null) {
            try {
                // Create a new User object without the transient password for storage
                val userToStore = user.copy(transientPasswordForOps = null)
                val userJson = json.encodeToString(userToStore)
                settings.putString(KEY_CURRENT_USER_DATA, userJson)
            } catch (e: Exception) {
                println("Error saving user to settings: ${e.message}")
                settings.remove(KEY_CURRENT_USER_DATA)
            }
        } else {
            settings.remove(KEY_CURRENT_USER_DATA)
        }
    }

    override fun getCurrentUser(): User? {
        val userJson = settings.getStringOrNull(KEY_CURRENT_USER_DATA)
        return if (userJson != null) {
            try {
                json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                println("Error reading user from settings: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    override fun clearAuthSettings() {
        settings.remove(KEY_LAST_LOGGED_IN_EMAIL)
        settings.remove(KEY_CURRENT_USER_DATA)
    }
}