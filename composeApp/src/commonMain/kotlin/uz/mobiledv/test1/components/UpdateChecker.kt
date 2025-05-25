package uz.mobiledv.test1.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.repository.AppUpdateViewModel
import uz.mobiledv.test1.repository.UpdateState

@Composable
fun UpdateChecker(
    appUpdateViewModel: AppUpdateViewModel = koinViewModel()
) {
    val updateState by appUpdateViewModel.updateState.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf<UpdateState.UpdateAvailable?>(null) }
    var showDownloadProgressDialog by remember { mutableStateOf<UpdateState.Downloading?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appUpdateViewModel.checkForUpdates()
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.UpdateAvailable -> {
                showUpdateDialog = state
            }
            is UpdateState.Downloading -> {
                showDownloadProgressDialog = state
                showUpdateDialog = null // Close update prompt if download started
            }
            is UpdateState.DownloadComplete -> {
                showDownloadProgressDialog = null
                snackbarHostState.showSnackbar("Update downloaded. Install prompt should appear.")
                // Install is triggered by the ViewModel
                appUpdateViewModel.resetUpdateState() // Or move to idle after install attempt
            }
            is UpdateState.Error -> {
                showUpdateDialog = null
                showDownloadProgressDialog = null
                snackbarHostState.showSnackbar("Update Error: ${state.message}", duration = SnackbarDuration.Long)
                appUpdateViewModel.resetUpdateState()
            }
            is UpdateState.NoUpdate -> {
                println("No update available or app is up to date.")
                // Optionally show a snackbar for "App is up to date"
                // snackbarHostState.showSnackbar("App is up to date.")
                appUpdateViewModel.resetUpdateState()
            }
            else -> { /* Idle, Checking */ }
        }
    }

    if (showUpdateDialog != null) {
        val versionInfo = (showUpdateDialog as UpdateState.UpdateAvailable).versionInfo
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog = null
                appUpdateViewModel.resetUpdateState()
            },
            title = { Text("Update Available!") },
            text = {
                Text(
                    "A new version (${versionInfo.versionName}) is available.\n\nRelease Notes:\n${versionInfo.releaseNotes ?: "No release notes provided."}"
                )
            },
            confirmButton = {
                Button(onClick = {
                    appUpdateViewModel.downloadAndInstallUpdate(versionInfo)
                    showUpdateDialog = null // Dialog will be replaced by progress or closes
                }) {
                    Text("Download & Install")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showUpdateDialog = null
                    appUpdateViewModel.resetUpdateState()
                }) {
                    Text("Later")
                }
            }
        )
    }

    if (showDownloadProgressDialog != null) {
        val progress = (showDownloadProgressDialog as UpdateState.Downloading).progress
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss download easily, or add cancel logic */ },
            title = { Text("Downloading Update...") },
            text = {
                LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Text("Progress: ${(progress * 100).toInt()}%")
            },
            confirmButton = {} // No buttons during download usually
        )
    }

    // Place SnackbarHost at a suitable level in your UI tree, e.g., in App.kt's Scaffold
    // For this example, it's standalone, but integrate it properly.
    // Box(modifier = Modifier.fillMaxSize()) { // Example placement
    //     SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    // }
}
