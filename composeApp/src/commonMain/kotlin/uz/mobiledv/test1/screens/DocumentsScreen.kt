@file:OptIn(ExperimentalMaterial3Api::class)

package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.mobiledv.test1.model.Document
// Import ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    folderId: String,
    onBackClick: () -> Unit,
    onDocumentClick: (Document) -> Unit, // Keep for now, or handle click within ViewModel
    viewModel: DocumentsViewModel = koinViewModel()
) {
    val documentsUiState by viewModel.documentsUiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Document?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Document?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load documents when folderId changes or screen is first composed
    LaunchedEffect(folderId, currentUser) {
        // Ensure user is loaded before trying to load documents if creation depends on user ID
        if (currentUser != null) {
            viewModel.loadDocuments(folderId)
        } else if (currentUser == null && viewModel.currentUser.value == null) {
            // This might indicate an issue or that user is still loading.
            // ViewModel's init should try to load user.
        }
    }

    LaunchedEffect(documentsUiState) {
        when (val state = documentsUiState) {
            is DocumentsUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents in Folder") }, // Potentially add folder name if available
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Document")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = documentsUiState) {
            is DocumentsUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DocumentsUiState.Success -> {
                if (state.documents.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No documents in this folder. Add one!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(state.documents) { document ->
                            DocumentItem(
                                document = document,
                                onClick = {
                                    // Decide if click navigates or shows details.
                                    // For now, it could be for viewing/downloading.
                                    onDocumentClick(document)
                                    // Example: viewModel.downloadDocument(document.appwriteFileId)
                                },
                                onEdit = { showEditDialog = document },
                                onDelete = { showDeleteDialog = document }
                                // isAdmin = currentUser?.isAdmin == true
                            )
                        }
                    }
                }
            }
            is DocumentsUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error loading documents.")
                    Button(onClick = { viewModel.loadDocuments(folderId) }) {
                        Text("Retry")
                    }
                }
            }
            is DocumentsUiState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Select a folder to see documents.")
                }
            }
        }
    }

    if (showAddDialog) {
        DocumentDialog( // Assuming you have a file picker mechanism for fileBytes
            onDismiss = { showAddDialog = false },
            onConfirm = { name, content -> // Simplified for text-only for now
                // In a real app, you'd get fileBytes, fileName, mimeType from a picker
                viewModel.createDocument(folderId, name, content, null, null, null)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { document ->
        DocumentDialog(
            document = document,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, content ->
                viewModel.updateDocument(document.id, folderId, name, content)
                showEditDialog = null
            }
        )
    }

    showDeleteDialog?.let { document ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete document \"${document.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(document.id, document.appwriteFileId, folderId)
                        showDeleteDialog = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DocumentItem(
    document: Document,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
    // isAdmin: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text( // Show content preview if it's short, or file type
                    text = document.mimeType ?: document.content.take(100),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Created: ${document.createdAt?.take(10)} by ${document.createdBy.take(8)}...",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            // if (isAdmin || document.createdBy == currentUserId) { // More granular permission
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "Edit Document")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete Document")
                }
            }
            // }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentDialog(
    document: Document? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, content: String) -> Unit // Add params for file if needed
    // onConfirm: (name: String, content: String, file: ByteArray?, fileName: String?, mimeType: String?) -> Unit
) {
    var name by remember { mutableStateOf(document?.name ?: "") }
    var content by remember { mutableStateOf(document?.content ?: "") } // For text part
    var nameError by remember { mutableStateOf<String?>(null) }
    // Add states for selected file if implementing file upload within dialog

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (document == null) "Add Document" else "Edit Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Document Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true
                )
                if (nameError != null) {
                    Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content / Description") }, // Or "Text Content"
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                // Add a button here to pick a file for upload if applicable
                // e.g., Button(onClick = { /* trigger file picker */ }) { Text("Select File") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Document name cannot be empty."
                        return@TextButton
                    }
                    // Pass fileBytes, selectedFileName, selectedMimeType if you have them
                    onConfirm(name, content)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}