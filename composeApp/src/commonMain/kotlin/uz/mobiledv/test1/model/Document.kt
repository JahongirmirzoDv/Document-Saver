package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val folderId: String,
    val fileUrl: String,
    val name: String,
    val content: String
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "folderId" to folderId,
            "fileUrl" to fileUrl,
            "name" to name,
            "content" to content
        )
    }
}

