package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uz.mobiledv.test1.components.DocumentItem
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.util.rememberFilePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: String,
    folderName: String,
    onNavigateBack: () -> Unit,
    viewModel: FoldersViewModel = koinViewModel { parametersOf(folderId) } // Pass folderId if needed for initial load
) {
    val documentsUiState by viewModel.folderDocumentsUiState.collectAsStateWithLifecycle()
    val fileUploadUiState by viewModel.fileUploadUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // File Picker Launcher
    val filePickerLauncher = rememberFilePickerLauncher { fileData ->
        if (fileData != null) {
            viewModel.uploadFileToFolder(folderId, fileData)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("File selection cancelled.")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDocumentsForFolder(folderId)
    }

    LaunchedEffect(fileUploadUiState) {
        when (val state = fileUploadUiState) {
            is FileUploadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearFileUploadStatus() // Clear status
                viewModel.loadDocumentsForFolder(folderId) // Refresh list
            }
            is FileUploadUiState.Error -> {
                snackbarHostState.showSnackbar("Upload Error: ${state.message}")
                viewModel.clearFileUploadStatus() // Clear status
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
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePickerLauncher() }) {
                Icon(Icons.Filled.CloudUpload, "Upload File")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
                            Icon(Icons.Filled.Description, contentDescription = "No documents", modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No documents yet. Tap '+' to upload.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            items(state.documents, key = { it.id }) { document ->
                                DocumentItem(document = document, onClick = {
                                    // Handle document click, e.g., open or download
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Clicked: ${document.name}")
                                    }
                                })
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
                        Text("Error: ${state.message}")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadDocumentsForFolder(folderId) }) {
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
                        Text("Initializing...")
                    }
                }
            }

            // Show progress for uploads
            if (fileUploadUiState is FileUploadUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Column (horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Uploading file...")
                    }
                }
            }
        }
    }
}