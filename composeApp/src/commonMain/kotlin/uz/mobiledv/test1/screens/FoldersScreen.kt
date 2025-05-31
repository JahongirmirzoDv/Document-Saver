// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FoldersScreen.kt
package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.isValidEmail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    appViewModel: AppViewModel = koinViewModel(),
    viewModel: FoldersViewModel = koinViewModel(),
    onFolderClick: (Folder) -> Unit,
    onLogout: () -> Unit,
    navController: NavController
) {
    // isManager determines if action buttons (add folder, create user) are shown.
    // Data visibility is for all authenticated users.
    val isManager by remember { derivedStateOf { appViewModel.isManager } }


    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showEditFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showCreateUserDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val foldersUiState by viewModel.foldersUiState.collectAsStateWithLifecycle()
    val folderOperationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()
    val userCreationAlert by appViewModel.operationAlert.collectAsStateWithLifecycle()

    // Load folders when the screen is first composed or if the current user changes
    // (e.g., after logout and login with a different user, though App.kt handles main navigation).
    // Or, if foldersUiState is Idle and user is authenticated.
    val currentUser = appViewModel.getCurrentUser()
    LaunchedEffect(Unit, currentUser) { // Re-load if user context changes or initially.
        if (currentUser != null && foldersUiState is FoldersUiState.Idle) {
            viewModel.loadFolders()
        } else if (currentUser == null && foldersUiState !is FoldersUiState.Error) {
            // If user becomes null (logged out), potentially clear folders or show error
            // This case should ideally be handled by navigation in App.kt redirecting to login.
        }
    }


    LaunchedEffect(folderOperationStatus) {
        folderOperationStatus?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearOperationStatus()
        }
    }

    LaunchedEffect(userCreationAlert) {
        userCreationAlert?.let { message ->
            snackbarHostState.showSnackbar(message)
            appViewModel.operationAlert.value = null
            if (message.startsWith("User") && (message.contains("created successfully") || message.contains(
                    "Error creating user"
                ))
            ) {
                if (!message.contains("already exists", ignoreCase = true) &&
                    !message.contains("Invalid", ignoreCase = true) &&
                    !message.contains("Password", ignoreCase = true) &&
                    !message.contains("cannot be empty", ignoreCase = true)
                ) {
                    showCreateUserDialog = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hujjatlar") }, // Unified title
                actions = {
                    if (isManager) { // Actions for managers (Desktop Admin)
                        IconButton(onClick = { showAddFolderDialog = true }) {
                            Icon(Icons.Filled.Add, "Add Folder")
                        }
                        IconButton(onClick = {
                            showCreateUserDialog = true
                        }) {
                            Icon(Icons.Filled.ManageAccounts, "Create User")
                        }
                    }
                    IconButton(onClick = {
                        onLogout() // Triggers AppViewModel.logout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize(),
            isRefreshing = foldersUiState is FoldersUiState.Loading, // Reflect ViewModel's loading state
            onRefresh = {
                viewModel.loadFolders() // Or a dedicated refreshFolders function
            },
            indicator = {
                Indicator(state = pullToRefreshState,
                    isRefreshing = foldersUiState is FoldersUiState.Loading,
                    modifier = Modifier.align(Alignment.BottomCenter))
            }
            // You can customize the indicator via the `indicator` parameter if needed
            // indicator = { PullToRefreshDefaults.Indicator(state = it) } // Default indicator
        ) {
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
                                // Text for all users if no folders exist.
                                Text(if (isManager) "No folders yet. Tap '+' to create one!" else "No folders available.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            ) {
                                items(state.folders, key = { it.id }) { folder ->
                                    FolderListItem(
                                        folder = folder,
                                        onClick = { onFolderClick(folder) },
                                        isManager = isManager, // Pass for edit/delete buttons
                                        onEdit = { if (isManager) showEditFolderDialog = folder },
                                        onDelete = {
                                            if (isManager) showDeleteFolderDialog = folder
                                        }
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
                            Text("Initializing folder view...")
                            // Optionally trigger loadFolders if authenticated and idle
                            // LaunchedEffect above handles this.
                        }
                    }
                }
            }
        }
    }

    // Dialogs remain conditional on isManager for CUD operations
    if (isManager) {
        if (showAddFolderDialog) {
            FolderDialog(
                onDismiss = { showAddFolderDialog = false },
                onConfirm = { name, description ->
                    viewModel.createFolder(name, description)
                    showAddFolderDialog = false
                }
            )
        }

        showEditFolderDialog?.let { folder ->
            FolderDialog(
                folder = folder,
                onDismiss = { showEditFolderDialog = null },
                onConfirm = { name, description ->
                    viewModel.updateFolder(folder.id, name, description)
                    showEditFolderDialog = null
                }
            )
        }

        showDeleteFolderDialog?.let { folder ->
            AlertDialog(
                onDismissRequest = { showDeleteFolderDialog = null },
                title = { Text("Delete Folder") },
                text = { Text("Are you sure you want to delete folder \"${folder.name}\"? This action cannot be undone and might delete its contents.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteFolder(folder.id)
                            showDeleteFolderDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteFolderDialog = null }) { Text("Cancel") }
                }
            )
        }

        if (showCreateUserDialog) {
            CreateUserDialog(
                onDismiss = { showCreateUserDialog = false },
                onConfirm = { username, email, password ->
                    // Manager creating a user. Can specify if they are admin or not.
                    // For simplicity, new users created by admin are not admins by default.
                    appViewModel.adminCreateUser(username, email, password, isAdmin = false)
                }
            )
        }
    }
}

@Composable
private fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    isManager: Boolean, // Controls visibility of edit/delete buttons
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
                // folder.createdAt?.let { // Optional: Display creation date or other metadata
                //    Text(
                //        text = "Created by user: ${folder.userId.take(8)}... on $it", // Example
                //        style = MaterialTheme.typography.labelSmall
                //    )
                // }
            }
            if (isManager) { // Edit and Delete buttons only for managers
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            "Edit Folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                    onValueChange = {
                        description = it
                        // No specific validation for description, but can be added if needed
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, email: String, pass: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New User") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = if (it.isNotBlank()) null else "Username cannot be empty."
                    },
                    label = { Text("Username*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = usernameError != null,
                    singleLine = true
                )
                usernameError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = if (isValidEmail(it)) null else "Invalid email format."
                    },
                    label = { Text("User Email*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = emailError != null,
                    singleLine = true
                )
                emailError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError =
                            if (it.length >= 6) null else "Password must be at least 6 characters."
                    },
                    label = { Text("Password*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = passwordError != null,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                passwordError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isUsernameValid = username.isNotBlank()
                    val isEmailCurrentlyValid = isValidEmail(email)
                    val isPasswordCurrentlyValid = password.length >= 6

                    usernameError = if (isUsernameValid) null else "Username cannot be empty."
                    emailError = if (isEmailCurrentlyValid) null else "Invalid email format."
                    passwordError =
                        if (isPasswordCurrentlyValid) null else "Password must be at least 6 characters."

                    if (isUsernameValid && isEmailCurrentlyValid && isPasswordCurrentlyValid) {
                        onConfirm(username.trim(), email.trim(), password)
                    }
                }
            ) { Text("Create User") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}