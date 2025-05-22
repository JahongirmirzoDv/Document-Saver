package uz.mobiledv.test1.mapper

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.SerializationException

// Import Appwrite SDK Models directly by their simple names
import io.appwrite.models.User
import io.appwrite.models.Preferences // This class is generic: Preferences<T>
import io.appwrite.models.Document
import io.appwrite.models.DocumentList
import io.appwrite.models.File
import io.appwrite.models.FileList

// Import KMP Models, aliasing those with conflicting simple names
import uz.mobiledv.test1.model.User as KmpUser
import uz.mobiledv.test1.model.UserPrefs // No conflict
import uz.mobiledv.test1.model.Folder    // No conflict
import uz.mobiledv.test1.model.Document as KmpModelDocument // Aliased to avoid conflict

val appwriteJsonMapper = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    // prettyPrint = true
}

private fun Any?.asJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is List<*> -> JsonArray(this.map { it.asJsonElement() })
    is Array<*> -> JsonArray(this.map { it.asJsonElement() })
    is Map<*, *> -> {
        val elementMap = mutableMapOf<String, JsonElement>()
        this.forEach { (key, value) ->
            if (key !is String) {
                throw SerializationException(
                    "Map keys must be of type String for JSON conversion. " +
                            "Encountered key: '$key' of type ${key?.let { it::class.simpleName }}"
                )
            }
            elementMap[key] = value.asJsonElement()
        }
        JsonObject(elementMap)
    }
    else -> throw SerializationException(
        "Cannot convert type ${this::class.simpleName} to JsonElement. Value: '$this'. "
    )
}

// --- User Mappers ---

// The Appwrite User model is generic: User<T> where T is the type of preferences.
// Appwrite Preferences model is also generic: Preferences<U> where U is the type of the data map.
// So, User prefs will be Preferences<Map<String, Any>>.
fun User<Preferences<Map<String, Any>>>.toKmpUser(): KmpUser { // Receiver is io.appwrite.models.User<Preferences<Map<String, Any>>>
    // this.prefs is of type Preferences<Map<String, Any>>?
    // this.prefs?.data is of type Map<String, Any>?
    val appwriteUserPrefsData: Map<String, Any>? = this.prefs.data.data
    var kmpUserPrefsInternal: UserPrefs? = null

    if (appwriteUserPrefsData != null && appwriteUserPrefsData.isNotEmpty()) {
        try {
            val prefsAsJsonObject: JsonObject = appwriteUserPrefsData.asJsonElement() as? JsonObject
                ?: JsonObject(emptyMap())

            if (prefsAsJsonObject.isNotEmpty()) {
                kmpUserPrefsInternal = appwriteJsonMapper.decodeFromJsonElement<UserPrefs>(prefsAsJsonObject)
            }
        } catch (e: Exception) {
            println("Warning processing UserPrefs for user ${this.id}: ${e.message} (Type: ${e::class.simpleName})")
        }
    }

    return KmpUser(
        id = this.id,
        username = this.name, // Appwrite User.name is String (non-null)
        email = this.email,
        isAdmin = kmpUserPrefsInternal?.isAdmin ?: false,
        password = null,
        phoneNumber = this.phone,
        prefs = kmpUserPrefsInternal
    )
}

// --- Folder Mappers ---

fun Document<Map<String, Any>>.toKmpFolder(): Folder {
    val combinedData: MutableMap<String, Any?> = this.data.toMutableMap()
    combinedData["\$id"] = this.id
    combinedData["\$collectionId"] = this.collectionId
    combinedData["\$databaseId"] = this.databaseId
    combinedData["\$createdAt"] = this.createdAt
    combinedData["\$updatedAt"] = this.updatedAt
    combinedData["\$permissions"] = this.permissions

    val folderAsJsonObject: JsonObject = combinedData.asJsonElement() as? JsonObject
        ?: JsonObject(emptyMap())

    return appwriteJsonMapper.decodeFromJsonElement<Folder>(folderAsJsonObject)
}

fun DocumentList<Map<String, Any>>.toKmpFolderList(): List<Folder> {
    return this.documents.map { it.toKmpFolder() }
}

data class KmpFolderListWrapper(
    val total: Long,
    val folders: List<Folder>
)

fun DocumentList<Map<String, Any>>.toKmpFolderListWrapper(): KmpFolderListWrapper {
    return KmpFolderListWrapper(
        total = this.total,
        folders = this.toKmpFolderList()
    )
}

// --- Document (File Metadata) Mappers ---

fun Document<Map<String, Any>>.toKmpDocument(): KmpModelDocument {
    val combinedData: MutableMap<String, Any?> = this.data.toMutableMap()
    combinedData["\$id"] = this.id
    combinedData["\$collectionId"] = this.collectionId
    combinedData["\$databaseId"] = this.databaseId
    combinedData["\$createdAt"] = this.createdAt
    combinedData["\$updatedAt"] = this.updatedAt
    combinedData["\$permissions"] = this.permissions

    val documentAsJsonObject: JsonObject = combinedData.asJsonElement() as? JsonObject
        ?: JsonObject(emptyMap())

    return appwriteJsonMapper.decodeFromJsonElement<KmpModelDocument>(documentAsJsonObject)
}

fun DocumentList<Map<String, Any>>.toKmpDocumentList(): List<KmpModelDocument> {
    return this.documents.map { it.toKmpDocument() }
}

data class KmpDocumentListWrapper(
    val total: Long,
    val documents: List<KmpModelDocument>
)

fun DocumentList<Map<String, Any>>.toKmpDocumentListWrapper(): KmpDocumentListWrapper {
    return KmpDocumentListWrapper(
        total = this.total,
        documents = this.toKmpDocumentList()
    )
}

// --- Appwrite Storage File Mapper ---

fun File.toKmpDocumentPartialUpdate(existingKmpDocument: KmpModelDocument): KmpModelDocument {
    return existingKmpDocument.copy(
        appwriteFileId = this.id,
        mimeType = this.mimeType
    )
}

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

fun File.toKmpAppwriteFileDetails(): KmpAppwriteFileDetails {
    val kmpPermissions: List<String>? = this.permissions?.mapNotNull { it as? String }

    val combinedData = mutableMapOf<String, Any?>()
    combinedData["\$id"] = this.id
    combinedData["\$bucketId"] = this.bucketId
    combinedData["\$createdAt"] = this.createdAt
    combinedData["\$updatedAt"] = this.updatedAt
    combinedData["\$permissions"] = kmpPermissions
    combinedData["name"] = this.name
    combinedData["signature"] = this.signature
    combinedData["mimeType"] = this.mimeType
    combinedData["sizeOriginal"] = this.sizeOriginal
    combinedData["chunksTotal"] = this.chunksTotal
    combinedData["chunksUploaded"] = this.chunksUploaded

    val fileDetailsAsJsonObject: JsonObject = combinedData.asJsonElement() as? JsonObject
        ?: JsonObject(emptyMap())

    return appwriteJsonMapper.decodeFromJsonElement<KmpAppwriteFileDetails>(fileDetailsAsJsonObject)
}

fun FileList.toKmpAppwriteFileDetailsList(): List<KmpAppwriteFileDetails> {
    return this.files.map { it.toKmpAppwriteFileDetails() }
}
