package uz.mobiledv.test1.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.model.UserPrefs
import uz.mobiledv.test1.model.Folder
import io.appwrite.models.Document as AppwriteSDKDocument
import io.appwrite.models.DocumentList as AppwriteSDKDocumentList
import io.appwrite.models.User as AppwriteSDKUser
import io.appwrite.models.FileList as AppwriteSDKFileList
import io.appwrite.models.File as AppwriteSDKFile

/**
 * Configured Json instance for kotlinx.serialization.
 * It's crucial that this configuration is consistent with how
 * Appwrite structures its JSON responses and how your KMP models are defined.
 */
val appwriteJsonMapper = Json {
    ignoreUnknownKeys = true // Very important for Appwrite, as responses may contain extra fields
    isLenient = true         // Helpful for minor inconsistencies
    coerceInputValues = true // If a default value is present in your model, use it if the key is missing
    // prettyPrint = true    // Useful for debugging, can be turned off for production
}

// --- User Mappers ---

/**
 * Converts an Appwrite SDK User object to your KMP User model.
 */
fun User.toKmpUser(): User {
    // Extract preferences. Appwrite stores prefs as Map<String, Any?>.
    // We need to safely cast and map this to your UserPrefs KMP model.
    val userPrefsData = this.prefs
    val kmpUserPrefs = if (userPrefsData != null) {
        try {
            // Convert the prefs map to a JsonElement, then decode to UserPrefs
            val prefsJsonElement = appwriteJsonMapper.encodeToJsonElement(userPrefsData)
            appwriteJsonMapper.decodeFromJsonElement<UserPrefs>(prefsJsonElement)
        } catch (e: Exception) {
            println("Warning: Could not parse UserPrefs for user ${this.id}: ${e.message}")
            null // Or a default UserPrefs instance
        }
    } else {
        null
    }

    return User(
        id = this.id,
        username = this.username ?: "", // Appwrite 'name' can be used as username
        email = this.email,
        isAdmin = kmpUserPrefs?.isAdmin ?: false, // Get isAdmin from mapped prefs
        password = null, // Password should not be mapped back from server response
        phoneNumber = this.phoneNumber,
        prefs = kmpUserPrefs
    )
}

// --- Folder Mappers ---

/**
 * Converts an Appwrite SDK Document (representing a Folder) to your KMP Folder model.
 */
fun Document.toKmpFolder(): Folder {
    // Combine system attributes and the 'data' map
    val combinedData = this.data.toMutableMap()
    combinedData["\$id"] = this.id
    combinedData["\$collectionId"] = this.collectionId
    combinedData["\$databaseId"] = this.databaseId
    combinedData["\$createdAt"] = this.createdAt
    combinedData["\$updatedAt"] = this.updatedAt
    combinedData["\$permissions"] = this.permissions

    val jsonElement = appwriteJsonMapper.encodeToJsonElement(combinedData)
    return appwriteJsonMapper.decodeFromJsonElement<Folder>(jsonElement)
}

/**
 * Converts an Appwrite SDK DocumentList (for Folders) to a List of your KMP Folder models.
 */
fun AppwriteSDKDocumentList.toKmpFolderList(): List<Folder> {
    return this.documents.map { it.toKmpFolder() }
}

/**
 * Wrapper class for Appwrite list responses if you need the total count.
 * Alternatively, your repository methods can return Pair<List<Folder>, Long>.
 */
data class KmpFolderListWrapper(
    val total: Long,
    val folders: List<Folder>
)

fun AppwriteSDKDocumentList.toKmpFolderListWrapper(): KmpFolderListWrapper {
    return KmpFolderListWrapper(
        total = this.total,
        folders = this.documents.map { it.toKmpFolder() }
    )
}


// --- Document (File Metadata) Mappers ---

/**
 * Converts an Appwrite SDK Document (representing file metadata) to your KMP Document model.
 */
fun AppwriteSDKDocument.toKmpDocument(): uz.mobiledv.test1.model.Document {
    val combinedData = this.data.toMutableMap()
    combinedData["\$id"] = this.id
    combinedData["\$collectionId"] = this.collectionId
    combinedData["\$databaseId"] = this.databaseId
    combinedData["\$createdAt"] = this.createdAt
    combinedData["\$updatedAt"] = this.updatedAt
    combinedData["\$permissions"] = this.permissions

    val jsonElement = appwriteJsonMapper.encodeToJsonElement(combinedData)
    return appwriteJsonMapper.decodeFromJsonElement<uz.mobiledv.test1.model.Document>(jsonElement)
}

/**
 * Converts an Appwrite SDK DocumentList (for file metadata) to a List of your KMP Document models.
 */
fun AppwriteSDKDocumentList.toKmpDocumentList(): List<uz.mobiledv.test1.model.Document> {
    return this.documents.map { it.toKmpDocument() }
}

/**
 * Wrapper class for Appwrite list responses if you need the total count.
 */
data class KmpDocumentListWrapper(
    val total: Long,
    val documents: List<uz.mobiledv.test1.model.Document>
)

fun AppwriteSDKDocumentList.toKmpDocumentListWrapper(): KmpDocumentListWrapper {
    return KmpDocumentListWrapper(
        total = this.total,
        documents = this.documents.map { it.toKmpDocument() }
    )
}


// --- Appwrite Storage File Mapper (if you need to map io.appwrite.models.File) ---
/**
 * Converts an Appwrite SDK File object (from Storage) to relevant fields
 * in your KMP Document model or a dedicated KMP File model if you create one.
 * This is an example if you fetch file details separately. Often, the appwriteFileId
 * stored in your KMP Document metadata is enough.
 */
fun AppwriteSDKFile.toKmpDocumentPartialUpdate(existingKmpDocument: uz.mobiledv.test1.model.Document): uz.mobiledv.test1.model.Document {
    return existingKmpDocument.copy(
        // id = existingKmpDocument.id, // Keep existing metadata ID
        // folderId = existingKmpDocument.folderId, // Keep existing
        name = this.name, // Update name from storage file if it's the source of truth
        appwriteFileId = this.id,
        mimeType = this.mimeType,
        // createdAt for the file itself, might be different from metadata createdAt
        // You might want a separate field in your KMP Document for file's own creation/modification date
    )
}

// You can also create a dedicated KMP model for Appwrite File details if needed:
@kotlinx.serialization.Serializable
data class KmpAppwriteFileDetails(
    @SerialName("\$id") val id: String,
    @SerialName("\$bucketId") val bucketId: String,
    @SerialName("\$createdAt") val createdAt: String,
    @SerialName("\$updatedAt") val updatedAt: String,
    @SerialName("\$permissions") val permissions: List<String>?,
    val name: String,
    val signature: String,
    val mimeType: String,
    val sizeOriginal: Long,
    val chunksTotal: Long,
    val chunksUploaded: Long
)

fun AppwriteSDKFile.toKmpAppwriteFileDetails(): KmpAppwriteFileDetails {
    val combinedData = mutableMapOf<String, Any?>()
    combinedData["\$id"] = this.id
    combinedData["\$bucketId"] = this.bucketId
    combinedData["\$createdAt"] = this.createdAt
    combinedData["\$updatedAt"] = this.updatedAt
    combinedData["\$permissions"] = this.permissions // Make sure this matches your model's type
    combinedData["name"] = this.name
    combinedData["signature"] = this.signature
    combinedData["mimeType"] = this.mimeType
    combinedData["sizeOriginal"] = this.sizeOriginal
    combinedData["chunksTotal"] = this.chunksTotal
    combinedData["chunksUploaded"] = this.chunksUploaded

    val jsonElement = appwriteJsonMapper.encodeToJsonElement(combinedData)
    return appwriteJsonMapper.decodeFromJsonElement<KmpAppwriteFileDetails>(jsonElement)
}

fun AppwriteSDKFileList.toKmpAppwriteFileDetailsList(): List<KmpAppwriteFileDetails> {
    return this.files.map { it.toKmpAppwriteFileDetails() }
}
