package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Document(
    var id: String = "", // Primary key, often UUID

    @SerialName("folder_id") // Foreign key to your 'folders' table
    var folderId: String = "",

    var name: String = "", // File name or document title

    // ID of the file in Supabase Storage. This links the metadata to the actual file.
    @SerialName("storage_file_path") // Or supabase_file_id, storage_object_name
    var storageFilePath: String? = null,

    // Optional: if you store text content directly in the database table
    var content: String? = "",

    @SerialName("user_id") // Or created_by
    var userId: String = "",

    @SerialName("created_at")
    var createdAt: String? = null,

    @SerialName("mime_type")
    var mimeType: String? = null
    // Permissions: handled by RLS
) {
    fun toSupabaseCreateData(): Map<String, Any?> {
        return mapOf(
            "folder_id" to folderId,
            "name" to name,
            "storage_file_path" to storageFilePath,
            "content" to content,
            "user_id" to userId,
            "mime_type" to mimeType
        ).filterValues { it != null }
    }
}
// AppwriteDocumentList is no longer needed.