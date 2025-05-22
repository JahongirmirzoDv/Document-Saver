package uz.mobiledv.test1.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    @SerialName("\$id") // Appwrite document ID
    var id: String = "",
    var name: String = "",
    var createdBy: String = "", // User ID of creator
    @SerialName("\$createdAt") // Appwrite timestamp
    var createdAt: String? = null, // Appwrite returns ISO 8601 string
    val description: String = "",
    // Add any other Appwrite system fields if needed, e.g., $updatedAt, $permissions
    @SerialName("\$permissions")
    val permissions: List<String>? = null
) {
    fun toAppwriteCreateData(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "createdBy" to createdBy,
            "description" to description
            // 'id' and 'createdAt' will be handled by Appwrite
        )
    }
}

// For Appwrite list responses
@Serializable
data class AppwriteFolderList(
    val total: Long,
    val documents: List<Folder>
)