package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val username: String = "",
    val isAdmin: Boolean = false,
    val password:String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf("id" to id, "username" to username, "isAdmin" to isAdmin)
    }
}

