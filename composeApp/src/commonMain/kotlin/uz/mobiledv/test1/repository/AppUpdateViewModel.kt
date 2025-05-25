package uz.mobiledv.test1.repository

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable // Ktor's extension for ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import uz.mobiledv.test1.model.AppVersionInfo
import uz.mobiledv.test1.model.getCurrentAppVersionCode
import uz.mobiledv.test1.model.triggerApkInstall
import uz.mobiledv.test1.util.PlatformType
import uz.mobiledv.test1.util.UpdateSaver
import uz.mobiledv.test1.util.getCurrentPlatform

const val APP_RELEASES_TABLE = "app_releases"
private const val DEFAULT_DOWNLOAD_BUFFER_SIZE = 8192 // 8KB, remains the same

@Stable
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val versionInfo: AppVersionInfo) : UpdateState()
    data object NoUpdate : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class DownloadComplete(val apkPath: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateViewModel(
    private val supabaseClient: SupabaseClient,
    private val fileSaver: UpdateSaver
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    fun checkForUpdates() {
        if (getCurrentPlatform() != PlatformType.ANDROID) {
            _updateState.value = UpdateState.NoUpdate
            return
        }

        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            try {
                val currentVersionCode = getCurrentAppVersionCode()
                if (currentVersionCode == 0) {
                    _updateState.value = UpdateState.Error("Could not get current app version.")
                    return@launch
                }

                val latestVersion = supabaseClient.postgrest[APP_RELEASES_TABLE]
                    .select {
                        order("version_code", Order.DESCENDING)
                        limit(1)
                    }.decodeSingleOrNull<AppVersionInfo>()

                if (latestVersion == null) {
                    _updateState.value = UpdateState.NoUpdate
                } else if (latestVersion.versionCode > currentVersionCode) {
                    _updateState.value = UpdateState.UpdateAvailable(latestVersion)
                } else {
                    _updateState.value = UpdateState.NoUpdate
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = UpdateState.Error("Error checking for updates: ${e.message}")
            }
        }
    }

    fun downloadAndInstallUpdate(versionInfo: AppVersionInfo) {
        if (getCurrentPlatform() != PlatformType.ANDROID) return

        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Downloading(0f)

                val (bucketName, objectPath) = versionInfo.apkStoragePath.split("/", limit = 2)
                if (bucketName.isBlank() || objectPath.isBlank()) {
                    _updateState.value = UpdateState.Error("Invalid APK storage path format.")
                    return@launch
                }

                val publicUrl = supabaseClient.storage[bucketName].publicUrl(objectPath)
                println("Downloading APK from: $publicUrl")

                val httpResponse = withContext(Dispatchers.IO) {
                    httpClient.get(publicUrl)
                }

                val byteChannel: ByteReadChannel = httpResponse.bodyAsChannel()
                val totalBytes = httpResponse.contentLength() ?: -1

                val fileName = "update_v${versionInfo.versionCode}.apk"
                val targetDir = "apk_updates"

                var bytesCopied = 0L
                val buffer = ByteArray(DEFAULT_DOWNLOAD_BUFFER_SIZE)

                val savedFilePath = fileSaver.saveFile(fileName, targetDir) { sink ->
                    while (!byteChannel.isClosedForRead) {
                        val read = byteChannel.readAvailable(buffer)
                        if (read <= 0) break
//                        sink.write(buffer, offset = 0, byteCount = read)
                        bytesCopied += read

                        if (totalBytes > 0) {
                            val progress = (bytesCopied.toFloat() / totalBytes).coerceIn(0f, 1f)
                            _updateState.value = UpdateState.Downloading(progress)
                        }
                    }
                    sink.flush()
                }

                if (savedFilePath != null) {
                    println("Download complete: $savedFilePath")
                    _updateState.value = UpdateState.DownloadComplete(savedFilePath)
                    triggerApkInstall(savedFilePath)
                } else {
                    _updateState.value = UpdateState.Error("Failed to save downloaded APK.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = UpdateState.Error("Error downloading update: ${e.message}")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
