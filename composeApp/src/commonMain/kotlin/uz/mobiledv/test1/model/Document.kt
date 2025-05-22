package uz.mobiledv.test1.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Document(
    @SerialName("\$id") // Appwrite document ID for metadata
    var id: String = "",
    var folderId: String = "",
    var name: String = "", // File name
    var appwriteFileId: String? = null, // ID of the file in Appwrite Storage
    var content: String = "", // For text-based documents, or metadata
    var createdBy: String = "", // User ID of creator
    @SerialName("\$createdAt")
    var createdAt: String? = null, // Appwrite timestamp (ISO 8601 string)
    var mimeType: String? = null, // Mime type of the uploaded file
    @SerialName("\$permissions")
    val permissions: List<String>? = null
) {
    fun toAppwriteCreateData(): Map<String, Any?> {
        return mapOf(
            "folderId" to folderId,
            "name" to name,
            "appwriteFileId" to appwriteFileId,
            "content" to content,
            "createdBy" to createdBy,
            "mimeType" to mimeType
        ).filterValues { it != null }
    }
}

// For Appwrite list responses
@Serializable
data class AppwriteDocumentList(
    val total: Long,
    val documents: List<Document>
)