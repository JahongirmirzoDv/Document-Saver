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

    // isManager determines if upload/delete actions are available.
    // Read access is for all authenticated users.
    val isManager by remember { derivedStateOf { appViewModel.isManager } }

    val fileDownloadUiState by viewModel.fileDownloadUiState.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberFilePickerLauncher { fileData ->
        if (fileData != null) {
            // Upload action is restricted to managers in FoldersViewModel
            viewModel.uploadFileToFolder(folderId, fileData)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("File selection cancelled.")
            }
        }
    }

    // Initial load of documents for the folder
    LaunchedEffect(folderId, appViewModel.getCurrentUserId()) { // Reload if folderId or user changes
        if(appViewModel.getCurrentUserId() != null) { // Only load if authenticated
            viewModel.loadDocumentsForFolder(folderId)
        }
    }


    if (fileDownloadUiState is FileDownloadUiState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Downloading file...")
            }
        }
    }

    LaunchedEffect(fileDownloadUiState) {
        when (val state = fileDownloadUiState) {
            is FileDownloadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearFileDownloadStatus()
            }

            is FileDownloadUiState.Error -> {
                snackbarHostState.showSnackbar("Download Error: ${state.message}")
                viewModel.clearFileDownloadStatus()
            }

            else -> Unit
        }
    }

    LaunchedEffect(fileUploadUiState) {
        when (val state = fileUploadUiState) {
            is FileUploadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearFileUploadStatus()
                if(appViewModel.getCurrentUserId() != null) viewModel.loadDocumentsForFolder(folderId) // Refresh list
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") // Updated Icon
                    }
                }
            )
        },
        floatingActionButton = {
            if (isManager) { // Upload FAB only for managers
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
            isRefreshing = documentsUiState is FolderDocumentsUiState.Loading, // Reflect ViewModel's loading state
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
                                // Text changes slightly based on whether user is manager or not
                                Text(if (isManager) "No documents yet. Tap '+' to upload." else "No documents in this folder.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            ) {
                                items(state.documents, key = { it.id }) { document ->
                                    DocumentItem(
                                        document = document,
                                        isManager = isManager, // Pass for delete button visibility
                                        onClick = {
                                            // Click on item could mean download for non-managers,
                                            // or view details/preview for managers.
                                            // For now, download is via explicit button in DocumentItem.
                                            // If manager clicks item, could show metadata or preview.
                                            // If non-manager clicks item, could also trigger download.
                                            // Let's keep primary download via the icon button for now for clarity.
                                            if (!isManager) {
                                                viewModel.downloadFile(document)
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Manager clicked: ${document.name}. (View/Edit actions can be added)")
                                                }
                                            }
                                        },
                                        onDeleteClick = { // This is specifically for the delete icon
                                            if (isManager) {
                                                // Consider adding a confirmation dialog before deleting
                                                viewModel.deleteDocument(document)
                                            }
                                        },
                                        onDownloadFile = { // This is for the download icon
                                            // Any authenticated user can download
                                            viewModel.downloadFile(document)
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
                                if(appViewModel.getCurrentUserId() != null) viewModel.loadDocumentsForFolder(folderId)
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
                            // LaunchedEffect above handles initial load if authenticated
                        }
                    }
                }

                if (fileUploadUiState is FileUploadUiState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize().align(Alignment.Center), // Ensure it's centered over content
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.padding(32.dp),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 8.dp, // or shadowElevation
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Uploading file...")
                            }
                        }
                    }
                }
            }
        }
    }
}