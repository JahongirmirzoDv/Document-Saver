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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onFolderClick: (Folder) -> Unit,
    onBackClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    viewModel: FoldersViewModel = koinViewModel()
) {
    val foldersUiState by viewModel.foldersUiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle() // For dialogs if needed

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Folder?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // ViewModel now loads data in its init block or via a specific load function
        // If you need to reload based on screen parameters, call viewModel.loadFolders(param)
    }

    // Handle UI state changes for messages
    LaunchedEffect(foldersUiState) {
        when (val state = foldersUiState) {
            is FoldersUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                viewModel.resetState() // Optionally reset to avoid re-showing on config change
            }
            else -> {} // Handle other states if needed
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders - Welcome ${currentUser?.username ?: "User"}") },
                navigationIcon = {
                    // Conditionally show back button if this isn't the first screen after login
                    // For now, assuming it's always there as per original.
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Folder")
                    }
                    if (currentUser?.isAdmin == true) { // Show only if admin
                        IconButton(onClick = onUserManagementClick) {
                            Icon(Icons.Filled.Person, "User Management")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = foldersUiState) {
            is FoldersUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FoldersUiState.Success -> {
                if (state.folders.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No folders yet. Create one!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                    ) {
                        items(state.folders) { folder ->
                            FolderItem(
                                folder = folder,
                                onClick = { onFolderClick(folder) },
                                onEdit = { showEditDialog = folder },
                                onDelete = { showDeleteDialog = folder },
                                // Pass isAdmin if edit/delete should be conditional
                                // isAdmin = currentUser?.isAdmin == true
                            )
                        }
                    }
                }
            }
            is FoldersUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error loading folders. Pull to refresh or check connection.")
                    // Optionally add a retry button
                    Button(onClick = { viewModel.loadFolders() }) {
                        Text("Retry")
                    }
                }
            }
            is FoldersUiState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Loading initial data...") // Or some placeholder
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showAddDialog) {
        FolderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description ->
                viewModel.createFolder(name, description)
                showAddDialog = false // Close dialog immediately
            }
        )
    }

    showEditDialog?.let { folder ->
        FolderDialog(
            folder = folder,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, description ->
                viewModel.updateFolder(folder.id, name, description)
                showEditDialog = null
            }
        )
    }

    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete folder \"${folder.name}\"? This will also delete all documents inside.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
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
private fun FolderItem(
    folder: Folder,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    // isAdmin: Boolean = false // Example if actions depend on role
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick) // Make the whole card clickable
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
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (folder.description.isNotEmpty()) {
                    Text(
                        text = folder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Created by: ${folder.createdBy.take(8)}... on ${folder.createdAt?.take(10)}", // Basic info
                    style = MaterialTheme.typography.labelSmall
                )
            }
            // Row for action buttons
            // if (isAdmin) { // Conditionally show buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "Edit Folder")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete Folder")
                }
            }
            // }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDialog(
    folder: Folder? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(folder?.name ?: "") }
    var description by remember { mutableStateOf(folder?.description ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Add Folder" else "Edit Folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null // Clear error when user types
                    },
                    label = { Text("Folder Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true
                )
                if (nameError != null) {
                    Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Folder name cannot be empty."
                        return@TextButton
                    }
                    onConfirm(name, description)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}