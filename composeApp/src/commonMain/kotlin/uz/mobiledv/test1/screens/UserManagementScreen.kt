@file:OptIn(ExperimentalMaterial3Api::class)

package uz.mobiledv.test1.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.util.isValidEmail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBackClick: () -> Unit,
    viewModel: UserManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<User?>(null) }
    var showDeleteDialog by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // ViewModel loads users in its init block
    }
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UserManagementUiState.Error -> {
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
        when (val state = uiState) {
            is UserManagementUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UserManagementUiState.Success -> {
                if (state.users.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No users found. Add one!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                    ) {
                        items(state.users) { user ->
                            UserItem(
                                user = user,
                                onEdit = { showEditDialog = user },
                                onDelete = { showDeleteDialog = user }
                            )
                        }
                    }
                }
            }
            is UserManagementUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error loading users.")
                    Button(onClick = { viewModel.loadUsers() }) {
                        Text("Retry")
                    }
                }
            }
            is UserManagementUiState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator() // Initial loading state
                }
            }
        }
    }

    if (showAddDialog) {
        UserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { username, email, password, isAdmin ->
                viewModel.createUser(username, email, password, isAdmin)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { user ->
        UserDialog(
            user = user,
            onDismiss = { showEditDialog = null },
            onConfirm = { username, email, password, isAdmin -> // Password is for new password
                viewModel.updateUser(user.id, username, email.ifBlank { null }, isAdmin)
                // Note: Password update might need a separate mechanism or be disallowed here
                showEditDialog = null
            }
        )
    }

    showDeleteDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete user ${user.username} (${user.email})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUser(user.id)
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
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = user.username.ifBlank { "(No username)" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.email ?: "(No email)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Admin: ${if (user.isAdmin) "Yes" else "No"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "ID: ${user.id}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "Edit User")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete User")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDialog(
    user: User? = null, // Existing user for editing, null for adding
    onDismiss: () -> Unit,
    onConfirm: (username: String, email: String, password: String, isAdmin: Boolean) -> Unit
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") } // Only for new user or changing password
    var isAdmin by remember { mutableStateOf(user?.isAdmin ?: false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Add New User" else "Edit User") },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; usernameError = null },
                    label = { Text("Username*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = usernameError != null,
                    singleLine = true
                )
                if (usernameError != null) {
                    Text(usernameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = { Text("Email*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = emailError != null,
                    singleLine = true
                )
                if (emailError != null) {
                    Text(emailError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = { Text(if (user == null) "Password*" else "New Password (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = passwordError != null,
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                if (passwordError != null) {
                    Text(passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it }
                    )
                    Text("Is Admin User")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var isValid = true
                    if (username.isBlank()) {
                        usernameError = "Username cannot be empty."
                        isValid = false
                    }
                    if (email.isBlank() || !isValidEmail(email)) { // Basic email validation
                        emailError = "Enter a valid email."
                        isValid = false
                    }
                    if (user == null && password.length < 8) { // Password required for new user and min length
                        passwordError = "Password must be at least 8 characters."
                        isValid = false
                    }
                    if (user != null && password.isNotEmpty() && password.length < 8) { // If changing password, also validate
                        passwordError = "New password must be at least 8 characters."
                        isValid = false
                    }

                    if (isValid) {
                        onConfirm(username, email, password, isAdmin)
                    }
                }
            ) { Text(if (user == null) "Create" else "Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}