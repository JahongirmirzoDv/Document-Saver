package uz.mobiledv.test1.data

import com.russhwolf.settings.Settings

// Using expect/actual for the factory to inject platform-specific context if needed,
// though multiplatform-settings often handles this.
// For simplicity with `multiplatform-settings-no-arg`, we can directly create Settings.
// If you needed context (like Android Context), an expect/actual factory would be better.

interface AuthSettings {
    fun saveLastLoggedInEmail(email: String?)
    fun getLastLoggedInEmail(): String?
    fun saveLoginStatus(isLoggedIn: Boolean)
    fun getLoginStatus(): Boolean
    fun clearAuthSettings()
}

internal const val KEY_LAST_LOGGED_IN_EMAIL = "last_logged_in_email"
internal const val KEY_IS_LOGGED_IN = "is_logged_in"


// We'll provide the platform-specific `Settings` instance via Koin
// and then create AuthSettingsImpl with it.

class AuthSettingsImpl(private val settings: Settings) : AuthSettings {

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

    override fun clearAuthSettings() {
        settings.remove(KEY_LAST_LOGGED_IN_EMAIL)
        settings.remove(KEY_IS_LOGGED_IN)
    }
}