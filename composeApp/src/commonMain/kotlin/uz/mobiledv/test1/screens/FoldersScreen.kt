@file:OptIn(ExperimentalMaterial3Api::class)

package uz.mobiledv.test1.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.FolderRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onFolderClick: (Folder) -> Unit,
    onBackClick: () -> Unit,
    currentUser: User?,
    onUserManagementClick: () -> Unit,
    folderRepository: FolderRepository
) {
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Folder?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            folders = folderRepository.getAllFolders()
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load folders: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Folder")
                    }
                    IconButton(onClick = onUserManagementClick) {
                        Icon(Icons.Filled.Person, "User Management")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(folders) { folder ->
                    FolderItem(
                        folder = folder,
                        onClick = { onFolderClick(folder) },
                        onEdit = { showEditDialog = folder },
                        onDelete = { showDeleteDialog = folder },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        FolderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                scope.launch {
                    try {
                        if (name.isBlank()) {
                            snackbarHostState.showSnackbar("Folder name cannot be empty")
                            return@launch
                        }
                        folderRepository.createFolder(name)
                        folders = folderRepository.getAllFolders()
                        showAddDialog = false
                        snackbarHostState.showSnackbar("Folder created successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to create folder: ${e.message}")
                    }
                }
            }
        )
    }

    showEditDialog?.let { folder ->
        FolderDialog(
            folder = folder,
            onDismiss = { showEditDialog = null },
            onConfirm = { name ->
                scope.launch {
                    try {
                        if (name.isBlank()) {
                            snackbarHostState.showSnackbar("Folder name cannot be empty")
                            return@launch
                        }
                        folderRepository.updateFolder(folder.id, name)
                        folders = folderRepository.getAllFolders()
                        showEditDialog = null
                        snackbarHostState.showSnackbar("Folder updated successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to update folder: ${e.message}")
                    }
                }
            }
        )
    }

    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete folder ${folder.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                folderRepository.deleteFolder(folder.id)
                                folders = folderRepository.getAllFolders()
                                showDeleteDialog = null
                                snackbarHostState.showSnackbar("Folder deleted successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to delete folder: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
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
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete")
                }
            }
        }
    }
}

@Composable
private fun FolderDialog(
    folder: Folder? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(folder?.name ?: "") }
    var showNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Add Folder" else "Edit Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    showNameError = false
                },
                label = { Text("Folder Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = showNameError
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                        return@TextButton
                    }
                    onConfirm(name)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 