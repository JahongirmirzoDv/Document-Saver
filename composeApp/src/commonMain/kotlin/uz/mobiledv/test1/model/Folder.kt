package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Folder(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    @SerialName("parent_id")
    var parentId: String? = null, // ID of the parent folder, null for root folders
    @SerialName("user_id")
    var userId: String = "",
    @SerialName("created_at")
    var createdAt: String? = null
) {
    fun toSupabaseCreateData(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "user_id" to userId,
            "parent_id" to parentId,
            // "created_at" is typically handled by Postgres default (e.g., now())
        ).filterValues { it != null } // Ensure null parentId is handled correctly by Supabase/Postgres
    }
}