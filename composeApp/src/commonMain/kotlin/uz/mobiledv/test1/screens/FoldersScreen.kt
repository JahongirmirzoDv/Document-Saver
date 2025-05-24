package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.PlatformType
import uz.mobiledv.test1.util.getCurrentPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    appViewModel: AppViewModel = koinViewModel(),
    viewModel: FoldersViewModel = koinViewModel(),
    onFolderClick: (Folder) -> Unit,
    onLogout: () -> Unit,
    navController: NavController // Added NavController
) {

    val currentPlatform = remember { getCurrentPlatform() } // Remember to avoid recomposition issues
    val isManager = currentPlatform == PlatformType.DESKTOP

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Folder?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // val scope = rememberCoroutineScope() // Not used here directly, but good to have if needed

    val foldersUiState by viewModel.foldersUiState.collectAsStateWithLifecycle()
    val operationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()

    LaunchedEffect(operationStatus) {
        operationStatus?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearOperationStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Folders") },
                actions = {
                    if (isManager) { // Only show Add for Desktop/Manager
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, "Add Folder")
                        }
                    }
                    IconButton(onClick = {
                        appViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = foldersUiState) {
                is FoldersUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Loading folders...")
                    }
                }

                is FoldersUiState.Success -> {
                    if (state.folders.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No folders yet. Tap '+' to create one!")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            items(state.folders, key = { it.id }) { folder ->
                                FolderListItem(
                                    folder = folder,
                                    onClick = { onFolderClick(folder) }, // Use callback
                                    isManager = isManager, // Pass manager status
                                    onEdit = { if (isManager) showEditDialog = folder },
                                    onDelete = { if (isManager) showDeleteDialog = folder }
                                )
                            }
                        }
                    }
                }

                is FoldersUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", maxLines = 4)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadFolders() }) {
                            Text("Retry")
                        }
                    }
                }

                is FoldersUiState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Initializing...")
                    }
                }
            }
        }
    }

    if (isManager) {
        if (showAddDialog) {
            FolderDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, description ->
                    viewModel.createFolder(name, description)
                    showAddDialog = false
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
                text = { Text("Are you sure you want to delete folder \"${folder.name}\"? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteFolder(folder.id)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    isManager: Boolean, // Added
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                folder.createdAt?.let {
                    Text(
                        // Basic date formatting, consider a more robust date parsing/formatting library for production
                        text = "Created: $it",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (isManager) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Edit, "Edit Folder", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            "Delete Folder",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
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
    var name by remember(folder) { mutableStateOf(folder?.name ?: "") }
    var description by remember(folder) { mutableStateOf(folder?.description ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Add New Folder" else "Edit Folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Folder Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true
                )
                if (nameError != null) {
                    Text(
                        nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Folder name cannot be empty."
                    } else {
                        nameError = null
                        onConfirm(name.trim(), description.trim())
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Preview needs NavController, which is tricky for simple previews.
// Consider a separate preview Composable that doesn't require full NavController.
// @Preview
// @Composable
// fun PreviewFoldersScreen() {
//    MaterialTheme {
//        FoldersScreen(
//            onFolderClick = {},
//            onLogout = {},
//            navController = rememberNavController() // This won't be fully functional in preview
//        )
//    }
// }