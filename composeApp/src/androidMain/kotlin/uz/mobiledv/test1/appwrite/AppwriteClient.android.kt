package uz.mobiledv.test1.appwrite

import io.appwrite.Client
import io.ktor.client.HttpClient
import android.content.Context
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Simple object to hold Android Context.
// Initialize this from your Application class or Activity.
object AppContextHolder {
    lateinit var context: Context

    fun isContextInitialized(): Boolean {
        return ::context.isInitialized
    }
}

actual fun createPlatformSpecificAppwriteClient(): Client {
    if (!AppContextHolder.isContextInitialized()) {
        throw IllegalStateException("AppContextHolder not initialized. Initialize it in your Application class.")
    }
    return Client()
}

actual fun createPlatformSpecificHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        expectSuccess = true // Important for Ktor to throw exceptions on non-2xx responses

        defaultRequest {
            url(APPWRITE_ENDPOINT) // Base URL for all requests
            contentType(ContentType.Application.Json)
            headers.append("X-Appwrite-Project", APPWRITE_PROJECT_ID)
            // X-Appwrite-Key for admin tasks (server-side)
            // X-Appwrite-Session for client-side (after login) - this needs to be added dynamically
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // Important for Appwrite models
            })
        }
        install(Logging) {
            level = LogLevel.ALL // Or LogLevel.BODY for production if needed
        }
    }
}