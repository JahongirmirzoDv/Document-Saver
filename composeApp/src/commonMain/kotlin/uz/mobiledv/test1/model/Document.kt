package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String = "",
    val folderId: String = "",
    val name: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "folderId" to folderId,
            "name" to name,
            "content" to content
        )
    }
}

