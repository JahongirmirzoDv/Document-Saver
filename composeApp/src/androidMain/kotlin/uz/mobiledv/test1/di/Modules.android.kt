package uz.mobiledv.test1.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.repository.DocumentRepository
import uz.mobiledv.test1.repository.DocumentRepositoryImpl
import uz.mobiledv.test1.screens.LoginViewModel

actual val platformModule = module {
    single<Settings> {
        // "auth_prefs" is the name of the SharedPreferences file
        SharedPreferencesSettings(
            delegate = androidContext().getSharedPreferences(
                "auth_prefs",
                Context.MODE_PRIVATE
            )
        )
    }
}