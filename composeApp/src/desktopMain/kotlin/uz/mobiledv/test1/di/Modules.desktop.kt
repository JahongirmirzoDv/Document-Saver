package uz.mobiledv.test1.di

import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.repository.DocumentRepository
import uz.mobiledv.test1.screens.LoginViewModel
import java.util.prefs.Preferences


actual val platformModule = module {
    // Provide the platform-specific Settings instance
    single<Settings> {
        Settings() // Use the library's factory function
    }
//    single<Settings> {
////         java.util.prefs.Preferences.userRoot().node("uz.mobiledv.test1.settings")
//        // You can customize the node name if needed
////        JavaPreferencesSettings(delegate = Preferences.userRoot().node("uz.mobiledv.test1.auth_prefs"))
//    }

}