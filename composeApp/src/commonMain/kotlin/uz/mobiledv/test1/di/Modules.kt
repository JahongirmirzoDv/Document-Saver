package uz.mobiledv.test1.di

import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.data.AuthSettingsImpl
import uz.mobiledv.test1.util.FileSaver

expect val platformModule: Module

expect val platformFileSaverModule: Module // New expect


val sharedModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true // Add this line
        }
    }
    single { KtorClientInstance.httpClient }

    single<AuthSettings> { AuthSettingsImpl(get(),get()) } // Koin will inject the platform-specific Settings
}