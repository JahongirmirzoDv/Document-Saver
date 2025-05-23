package uz.mobiledv.test1.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    // Supabase GoTrue typically uses a UUID string for user IDs.
    // This might come from the `id` field of the Supabase User object.
    val id: String = "",
    var username: String? = null, // Or 'displayName', often stored in user_metadata or a separate 'profiles' table
    val email: String? = null,

    // 'isAdmin' would typically be managed via a custom claim, a separate 'profiles' table,
    // or PostgreSQL Row Level Security (RLS) policies.
    // Let's assume it's part of a 'profiles' table or user_metadata for now.
    var isAdmin: Boolean = false,

    // Password is not typically stored in the client-side User model after auth.
    // It's used for sign-up/sign-in.
    @Serializable(with = SensitiveStringSerializer::class)
    val password: String? = null, // Only for sign-up/sign-in operations

    val phoneNumber: String? = null, // Supabase supports phone auth

    // Custom user preferences or profile data might be stored in a separate 'profiles' table
    // linked by the user_id, or in `user_metadata`.
    val userMetadata: UserMetadata? = null // Example for custom data
)

@Serializable
data class UserMetadata( // Example structure for data in user_metadata or profiles table
    val displayName: String? = null,
    val isAdmin: Boolean? = null
    // Add other custom fields
)

// SensitiveStringSerializer remains useful for not serializing password.
object SensitiveStringSerializer : kotlinx.serialization.KSerializer<String?> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "SensitiveString",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: String?) {
        encoder.encodeNull()
    }
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): String? = decoder.decodeString()
}

// Appwrite specific models like AppwriteUser, AppwriteSession are no longer needed.
// Supabase client provides its own User and Session models from io.github.jan.supabase.gotrue.user.UserSession etc.