package uz.mobiledv.test1.appwrite

import io.appwrite.Client
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createPlatformSpecificAppwriteClient(): Client {
    return Client() // For desktop, context is not typically needed for basic client
}

actual fun createPlatformSpecificHttpClient(): HttpClient {
    return HttpClient(CIO) { // CIO engine for Desktop
        expectSuccess = true

        defaultRequest {
            url(APPWRITE_ENDPOINT)
            contentType(ContentType.Application.Json)
            headers.append("X-Appwrite-Project", APPWRITE_PROJECT_ID)
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }
}