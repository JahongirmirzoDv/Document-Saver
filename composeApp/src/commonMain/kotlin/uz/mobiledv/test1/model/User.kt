package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Your application's User model
@Serializable
data class User(
    @SerialName("\$id") // Map to Appwrite's user ID when fetched
    val id: String = "", // This will hold Appwrite's user ID after creation/fetch
    val username: String = "", // Can map to Appwrite's 'name' or be a custom field
    val email: String? = null, // Appwrite uses email for auth typically
    val isAdmin: Boolean = false,
    // Password should not be stored in this model after fetching. It's for creation only.
    @Serializable(with = SensitiveStringSerializer::class) // Example for not serializing password back
    val password: String? = null,
    val phoneNumber: String? = null, // Store if needed
    @SerialName("prefs")
    val prefs: UserPrefs? = null // For storing custom attributes like isAdmin
) {
    // For Appwrite user creation, 'name' can be 'username'
    fun toAppwriteCreateMap(userId: String = "unique()"): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "email" to email,
            "password" to password,
            "name" to username,
            "phone" to phoneNumber
        ).filterValues { it != null }
    }
}

@Serializable
data class UserPrefs(
    val isAdmin: Boolean? = null,
    val customUsername: String? = null // if username is different from Appwrite name
    // Add other custom preferences here
)

// A simple way to prevent password from being serialized in responses if included in the model
object SensitiveStringSerializer : kotlinx.serialization.KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor(
        "SensitiveString",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: String?) {
        encoder.encodeNull()
    } // Always encode as null or omit

    override fun deserialize(decoder: Decoder): String? = decoder.decodeString()
}

// Appwrite's User model representation (simplified for what we might get back)
// You can also use io.appwrite.models.User directly if using the SDK
@Serializable
data class AppwriteUser(
    @SerialName("\$id")
    val id: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val emailVerification: Boolean,
    val phoneVerification: Boolean,
    val prefs: UserPrefs? // Using our UserPrefs model for deserialization
)

// For login
@Serializable
data class AppwriteSession(
    @SerialName("\$id")
    val id: String,
    @SerialName("userId")
    val userId: String,
    // ... other session fields
)
