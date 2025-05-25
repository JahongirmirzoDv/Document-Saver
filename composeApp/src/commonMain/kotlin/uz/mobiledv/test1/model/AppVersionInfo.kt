package uz.mobiledv.test1.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionInfo(
    val id: String, // UUID
    @SerialName("version_code")
    val versionCode: Int,
    @SerialName("version_name")
    val versionName: String,
    @SerialName("apk_storage_path")
    val apkStoragePath: String, // e.g., "app-releases/my_app_v1.2.0.apk"
    @SerialName("release_notes")
    val releaseNotes: String? = null,
    @SerialName("published_at")
    val publishedAt: String // kotlinx-datetime Instant string or similar
)