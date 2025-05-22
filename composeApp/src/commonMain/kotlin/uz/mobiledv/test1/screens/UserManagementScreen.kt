@file:OptIn(ExperimentalMaterial3Api::class)

package uz.mobiledv.test1.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.DocumentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBackClick: () -> Unit,

) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<User?>(null) }
    var showDeleteDialog by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            isLoading = true
//            users = userRepository.getAllUsers()
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load users: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add User")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(users) { user ->
                UserItem(
                    user = user,
                    onEdit = { showEditDialog = user },
                    onDelete = { showDeleteDialog = user }
                )
            }
        }
    }

    if (showAddDialog) {
        UserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { username, password ->
                scope.launch {
                    try {
                        if (username.isBlank()) {
                            snackbarHostState.showSnackbar("Username cannot be empty")
                            return@launch
                        }
                        if (password.isBlank()) {
                            snackbarHostState.showSnackbar("Password cannot be empty")
                            return@launch
                        }
//                        userRepository.createUser(username, password)
//                        users = userRepository.getAllUsers()
                        showAddDialog = false
                        snackbarHostState.showSnackbar("User created successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to create user: ${e.message}")
                    }
                }
            }
        )
    }

    showEditDialog?.let { user ->
        UserDialog(
            user = user,
            onDismiss = { showEditDialog = null },
            onConfirm = { username, password ->
                scope.launch {
                    try {
                        if (username.isBlank()) {
                            snackbarHostState.showSnackbar("Username cannot be empty")
                            return@launch
                        }
//                        userRepository.updateUser(user.id, username, password)
//                        users = userRepository.getAllUsers()
                        showEditDialog = null
                        snackbarHostState.showSnackbar("User updated successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to update user: ${e.message}")
                    }
                }
            }
        )
    }

    showDeleteDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete user ${user.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
//                                userRepository.deleteUser(user.id)
//                                users = userRepository.getAllUsers()
                                showDeleteDialog = null
                                snackbarHostState.showSnackbar("User deleted successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to delete user: ${e.message}")
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
private fun UserItem(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            Column {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text =  "User",
                    style = MaterialTheme.typography.bodyMedium
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
private fun UserDialog(
    user: User? = null,
    onDismiss: () -> Unit,
    onConfirm: (username: String, password: String) -> Unit
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var showPasswordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Add User" else "Edit User") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = username.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        showPasswordError = false
                    },
                    label = { Text(if (user == null) "Password" else "New Password (leave blank to keep current)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showPasswordError
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Admin User")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isBlank()) {
                        return@TextButton
                    }
                    if (user == null && password.isBlank()) {
                        showPasswordError = true
                        return@TextButton
                    }
                    onConfirm(username, password)
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