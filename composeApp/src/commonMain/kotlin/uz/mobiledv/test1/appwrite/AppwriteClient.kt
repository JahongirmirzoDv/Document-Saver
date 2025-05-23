package uz.mobiledv.test1.appwrite

import io.appwrite.Client
import io.appwrite.services.Account
import io.ktor.client.HttpClient

// Appwrite Configuration - Replace with your actual project details
const val APPWRITE_ENDPOINT = "https://fra.cloud.appwrite.io/v1" // Or your self-hosted endpoint
const val APPWRITE_PROJECT_ID = "682fee890011207f8d2f"
const val APPWRITE_DATABASE_ID = "682f06e5000a0e13b023" // For folders and documents metadata
const val APPWRITE_USER_PROFILES_COLLECTION_ID = "user_profiles" // If you need extra user fields
const val APPWRITE_FOLDERS_COLLECTION_ID = "folders"
const val APPWRITE_DOCUMENTS_COLLECTION_ID = "documents"
const val APPWRITE_FILES_BUCKET_ID = "files" // For document file storage


// expect declaration for the Appwrite client
expect fun createPlatformSpecificAppwriteClient(): Client

// expect declaration for Ktor HttpClient
expect fun createPlatformSpecificHttpClient(): HttpClient

// Singleton for Appwrite Client
object AppwriteInstance {
    val client: Client by lazy {
        createPlatformSpecificAppwriteClient().apply {
            setEndpoint(APPWRITE_ENDPOINT)
            setProject(APPWRITE_PROJECT_ID)
            // For self-signed certificates (development only, not recommended for production)
             setSelfSigned(true)

        }
    }
}

// Singleton for Ktor HttpClient
object KtorClientInstance {
    val httpClient: HttpClient by lazy {
        createPlatformSpecificHttpClient()
    }
}