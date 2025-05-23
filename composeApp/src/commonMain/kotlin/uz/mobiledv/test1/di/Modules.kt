package uz.mobiledv.test1.di

import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.data.AuthSettingsImpl

expect val platformModule: Module



val sharedModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
    single { KtorClientInstance.httpClient }

    single<AuthSettings> { AuthSettingsImpl(get(),get()) } // Koin will inject the platform-specific Settings
}