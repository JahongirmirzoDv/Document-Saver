package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf("id" to id, "name" to name, "createdBy" to createdBy, "createdAt" to createdAt)
    }
}

