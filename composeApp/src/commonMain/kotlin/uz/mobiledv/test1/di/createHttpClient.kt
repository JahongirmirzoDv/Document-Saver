package uz.mobiledv.test1.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module


// expect declaration for Ktor HttpClient
expect fun createPlatformSpecificHttpClient(): HttpClient

// Singleton for Ktor HttpClient
object KtorClientInstance {
    val httpClient: HttpClient by lazy {
        createPlatformSpecificHttpClient()
    }
}