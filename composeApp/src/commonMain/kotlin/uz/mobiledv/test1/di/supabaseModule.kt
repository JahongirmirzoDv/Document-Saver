package uz.mobiledv.test1.di

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.SettingsResumableCache
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import org.koin.dsl.module


const val BUCKET = "test"
const val DOCUMENT = "documents"
const val FOLDER = "folders"

const val USERS_TABLE = "users" // Added users table
const val URL = "https://xovedzejjcuoqzrqbyzf.supabase.co"
const val KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhvdmVkemVqamN1b3F6cnFieXpmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwMTIxMjAsImV4cCI6MjA2MzU4ODEyMH0.bAvwpKnLijAS2kvrXOwM4QBqulsRUbdE91KEWnwq2b0"
val supabaseStorageModule = module {

    single {
        createSupabaseClient(
            supabaseUrl = URL,
            supabaseKey = KEY
        ) {
            install(Storage) {
                resumable {
                    cache = SettingsResumableCache()
                }
            }
            install(Auth) {
                platformGoTrueConfig()
            }

            defaultLogLevel = LogLevel.DEBUG
            install(Postgrest)
            install(Realtime)
        }
    }
    single {
        get<SupabaseClient>().storage[BUCKET].resumable
    }
}

expect fun AuthConfig.platformGoTrueConfig()