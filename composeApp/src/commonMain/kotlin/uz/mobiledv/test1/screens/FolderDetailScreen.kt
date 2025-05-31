// File: jahongirmirzodv/test.1.2/Test.1.2-ce174411ff66ed9510ec8cd734b4eb2fd3d73d03/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FolderDetailScreen.kt

package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Updated import
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// import androidx.compose.material3.BasicAlertDialog // Not used directly
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.window.DialogProperties // Not used directly
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// import androidx.navigation.NavController // Not used directly
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
// import org.koin.core.parameter.parametersOf // Not used directly
import uz.mobiledv.test1.AppViewModel // To check isManager
import uz.mobiledv.test1.components.DocumentItem
// import uz.mobiledv.test1.model.Document // Used by DocumentItem
// import uz.mobiledv.test1.util.PlatformType // Not used directly for isManager check logic here
// import uz.mobiledv.test1.util.getCurrentPlatform // Not used directly
import uz.mobiledv.test1.util.rememberFilePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: String,
    folderName: String,
    onNavigateBack: () -> Unit,
    viewModel: FoldersViewModel = koinViewModel(),
    appViewModel: AppViewModel = koinViewModel() // Inject AppViewModel
) {
    val documentsUiState by viewModel.folderDocumentsUiState.collectAsStateWithLifecycle()
    val fileUploadUiState by viewModel.fileUploadUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isManager by remember { derivedStateOf { appViewModel.isManager } }
    val fileDownloadUiState by viewModel.fileDownloadUiState.collectAsStateWithLifecycle() // For cache/open
    val filePublicDownloadUiState by viewModel.filePublicDownloadUiState.collectAsStateWithLifecycle() // New: for public downloads

    val filePickerLauncher = rememberFilePickerLauncher { fileData ->
        if (fileData != null) {
            viewModel.uploadFileToFolder(folderId, fileData)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("File selection cancelled or invalid type.")
            }
        }
    }

    LaunchedEffect(folderId, appViewModel.getCurrentUserId()) {
        if (appViewModel.getCurrentUserId() != null) {
            viewModel.loadDocumentsForFolder(folderId)
        }
    }

    // LaunchedEffect for cache/open download state
    LaunchedEffect(fileDownloadUiState) {
        when (val state = fileDownloadUiState) {
            is FileDownloadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearFileDownloadStatus() // Clear this specific state
            }
            is FileDownloadUiState.Error -> {
                snackbarHostState.showSnackbar("Open Error: ${state.message}")
                viewModel.clearFileDownloadStatus()
            }
            is FileDownloadUiState.Downloading -> {
                // Can show a distinct message or rely on the general "Loading"
            }
            else -> Unit
        }
    }

    // New LaunchedEffect for public download state
    LaunchedEffect(filePublicDownloadUiState) {
        when (val state = filePublicDownloadUiState) {
            is FilePublicDownloadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearFilePublicDownloadStatus() // Clear this specific state
            }
            is FilePublicDownloadUiState.Error -> {
                snackbarHostState.showSnackbar("Download Error: ${state.message}")
                viewModel.clearFilePublicDownloadStatus()
            }
            is FilePublicDownloadUiState.Downloading -> {
                // Could show a persistent snackbar or a dialog for progress if desired
                // For now, just a transient message:
                // snackbarHostState.showSnackbar("Downloading ${state.fileName}: ${(state.progress * 100).toInt()}%")
            }
            else -> Unit
        }
    }


    LaunchedEffect(fileUploadUiState) {
        when (val state = fileUploadUiState) {
            is FileUploadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearFileUploadStatus()
                if (appViewModel.getCurrentUserId() != null) viewModel.loadDocumentsForFolder(
                    folderId
                ) // Refresh list
            }

            is FileUploadUiState.Error -> {
                snackbarHostState.showSnackbar("Upload Error: ${state.message}")
                viewModel.clearFileUploadStatus()
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isManager) {
                FloatingActionButton(onClick = { filePickerLauncher() }) {
                    Icon(Icons.Filled.CloudUpload, "Upload File")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize(),
            isRefreshing = documentsUiState is FolderDocumentsUiState.Loading,
            onRefresh = {
                viewModel.loadDocumentsForFolder(folderId)
            }
        ) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (val state = documentsUiState) {
                    is FolderDocumentsUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text("Loading documents...")
                        }
                    }

                    is FolderDocumentsUiState.Success -> {
                        if (state.documents.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    contentDescription = "No documents",
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (isManager) "No documents yet. Tap '+' to upload." else "No documents in this folder.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(state.documents, key = { it.id }) { document ->
                                    DocumentItem(
                                        document = document,
                                        isManager = isManager,
                                        onClick = { // Clicking item opens/views (downloads to cache first)
                                            viewModel.downloadAndOpenFile(document)
                                        },
                                        onDeleteClick = {
                                            if (isManager) {
                                                viewModel.deleteDocument(document)
                                            }
                                        },
                                        onDownloadToPublicClick = { // New action for download button
                                            viewModel.downloadAndSaveToPublicDownloads(document)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is FolderDocumentsUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Error: ${state.message}", maxLines = 3)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                if (appViewModel.getCurrentUserId() != null) viewModel.loadDocumentsForFolder(
                                    folderId
                                )
                            }) {
                                Text("Retry")
                            }
                        }
                    }

                    is FolderDocumentsUiState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Initializing document view...")
                        }
                    }
                }

                // Loading indicator for general operations (upload, public download, cache download)
                val isLoadingPublicDownload = filePublicDownloadUiState is FilePublicDownloadUiState.Downloading
                val isLoadingCacheDownload = fileDownloadUiState is FileDownloadUiState.Downloading || fileDownloadUiState is FileDownloadUiState.Loading
                val isLoadingUpload = fileUploadUiState is FileUploadUiState.Loading

                if (isLoadingPublicDownload || isLoadingCacheDownload || isLoadingUpload) {
                    val message = when {
                        isLoadingPublicDownload -> "Downloading to public folder..."
                        isLoadingCacheDownload -> "Preparing file to open..."
                        isLoadingUpload -> "Uploading file..."
                        else -> "Processing..."
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.padding(32.dp),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(message)
                            }
                        }
                    }
                }
            }
        }
    }
}