package uz.mobiledv.test1.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import java.util.prefs.Preferences


actual val platformModule = module {
    single<Settings> {
        // For storing preferences on desktop using java.util.prefs.Preferences
        PreferencesSettings(delegate = Preferences.userRoot().node("uz.mobiledv.test1.auth_prefs"))
    }
}