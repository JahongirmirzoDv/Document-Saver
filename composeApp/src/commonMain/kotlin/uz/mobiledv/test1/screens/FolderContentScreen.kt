package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder // Icon for add folder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder // Default folder icon
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.components.DocumentItem
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.util.FileData
import uz.mobiledv.test1.util.isValidEmail
import uz.mobiledv.test1.util.rememberFilePickerLauncher
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Helper for navigation arguments
fun encodeNavArg(arg: String?): String = arg?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: "null"
fun decodeNavArg(arg: String?): String? = if (arg == "null") null else arg?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentsScreen(
    navController: NavController,
    currentFolderId: String?, // Null for root
    currentFolderName: String?, // Null for root, used for App Bar title
    appViewModel: AppViewModel = koinViewModel(),
    foldersViewModel: FoldersViewModel = koinViewModel()
) {
    val isManager by remember { derivedStateOf { appViewModel.isManager } }
    val folderContentState by foldersViewModel.folderContentUiState.collectAsStateWithLifecycle()
    val fileUploadState by foldersViewModel.fileUploadUiState.collectAsStateWithLifecycle()
    val fileDownloadState by foldersViewModel.fileDownloadUiState.collectAsStateWithLifecycle() // For cache/open
    val filePublicDownloadState by foldersViewModel.filePublicDownloadUiState.collectAsStateWithLifecycle() // For public downloads
    val operationStatus by foldersViewModel.operationStatus.collectAsStateWithLifecycle() // For folder CUD messages
    val userCreationAlert by appViewModel.operationAlert.collectAsStateWithLifecycle() // For user creation messages


    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showEditFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }


    val filePickerLauncher = rememberFilePickerLauncher { fileData ->
        if (fileData != null && currentFolderId != null) { // Ensure we are inside a folder to upload
            foldersViewModel.uploadFileToFolder(currentFolderId, fileData)
        } else if (fileData == null) {
            scope.launch { snackbarHostState.showSnackbar("File selection cancelled or invalid type.") }
        } else if (currentFolderId == null) {
            scope.launch { snackbarHostState.showSnackbar("Cannot upload files to the root. Please select a folder.") }
        }
    }

    LaunchedEffect(currentFolderId, appViewModel.getCurrentUser()?.id) {
        foldersViewModel.loadFolderContents(currentFolderId)
    }

    // Handle various operation statuses with Snackbars
    LaunchedEffect(fileUploadState) {
        when (val state = fileUploadState) {
            is FileUploadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                foldersViewModel.clearFileUploadStatus()
            }
            is FileUploadUiState.Error -> {
                snackbarHostState.showSnackbar("Upload Error: ${state.message}")
                foldersViewModel.clearFileUploadStatus()
            }
            else -> Unit
        }
    }
    LaunchedEffect(fileDownloadState) {
        when (val state = fileDownloadState) {
            is FileDownloadUiState.Success -> {
                // Message about opening is handled by viewmodel, or can add snackbar here
                // snackbarHostState.showSnackbar(state.message)
                foldersViewModel.clearFileDownloadStatus()
            }
            is FileDownloadUiState.Error -> {
                snackbarHostState.showSnackbar("Open Error: ${state.message}")
                foldersViewModel.clearFileDownloadStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(filePublicDownloadState) {
        when (val state = filePublicDownloadState) {
            is FilePublicDownloadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                foldersViewModel.clearFilePublicDownloadStatus()
            }
            is FilePublicDownloadUiState.Error -> {
                snackbarHostState.showSnackbar("Download Error: ${state.message}")
                foldersViewModel.clearFilePublicDownloadStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(operationStatus) {
        operationStatus?.let {
            snackbarHostState.showSnackbar(it)
            foldersViewModel.clearOperationStatus()
        }
    }
    LaunchedEffect(userCreationAlert) {
        userCreationAlert?.let { message ->
            snackbarHostState.showSnackbar(message)
            appViewModel.operationAlert.value = null // Consume alert
            if (message.startsWith("User") && (message.contains("created successfully") || message.contains("Error creating user"))) {
                if (!message.contains("already exists", ignoreCase = true) &&
                    !message.contains("Invalid", ignoreCase = true) &&
                    !message.contains("Password", ignoreCase = true) &&
                    !message.contains("cannot be empty", ignoreCase = true)
                ) {
                    showCreateUserDialog = false // Close dialog on success/generic error
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolderName ?: "Hujjatlar") },
                navigationIcon = {
                    if (currentFolderId != null) { // Show back arrow if not in root
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    if (isManager && currentFolderId == null) { // Only show "Create User" at root for managers
                        IconButton(onClick = { showCreateUserDialog = true }) {
                            Icon(Icons.Filled.ManageAccounts, "Create User")
                        }
                    }
                    IconButton(onClick = { appViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (isManager) { // Create Folder FAB for managers
                    FloatingActionButton(
                        onClick = { showAddFolderDialog = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, "Create New Folder")
                    }
                }
                if (isManager && currentFolderId != null) { // Upload File FAB only if inside a folder for managers
                    FloatingActionButton(onClick = { filePickerLauncher() }) {
                        Icon(Icons.Filled.CloudUpload, "Upload File")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            state = pullRefreshState,
            isRefreshing = folderContentState is FolderContentUiState.Loading || fileUploadState is FileUploadUiState.Loading,
            onRefresh = { foldersViewModel.loadFolderContents(currentFolderId) },
        ) {
            when (val state = folderContentState) {
                is FolderContentUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Loading...")
                    }
                }
                is FolderContentUiState.Success -> {
                    val combinedItems = state.subFolders + state.documents
                    if (combinedItems.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Folder, "Empty folder", modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(if (isManager) "This folder is empty. Add a subfolder or upload files." else "This folder is empty.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(combinedItems, key = { item ->
                                when (item) {
                                    is Folder -> "folder-${item.id}"
                                    is Document -> "doc-${item.id}"
                                    else -> uuid4().toString() // Should not happen
                                }
                            }) { item ->
                                when (item) {
                                    is Folder -> FolderListItem(
                                        folder = item,
                                        isManager = isManager,
                                        onClick = {
                                            navController.navigate("folderContents/${encodeNavArg(item.id)}/${encodeNavArg(item.name)}")
                                        },
                                        onEditClick = { if (isManager) showEditFolderDialog = item },
                                        onDeleteClick = { if (isManager) showDeleteFolderDialog = item }
                                    )
                                    is Document -> DocumentItem(
                                        document = item,
                                        isManager = isManager,
                                        onClick = { foldersViewModel.downloadAndOpenFile(item) },
                                        onDeleteClick = {
                                            if (isManager) {
                                                // Optional: Show confirmation dialog for document deletion
                                                foldersViewModel.deleteDocument(item) // Assuming deleteDocument is in ViewModel
                                            }
                                        },
                                        onDownloadToPublicClick = { foldersViewModel.downloadAndSaveToPublicDownloads(item) }
                                    )
                                }
                            }
                        }
                    }
                }
                is FolderContentUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", maxLines = 4)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { foldersViewModel.loadFolderContents(currentFolderId) }) {
                            Text("Retry")
                        }
                    }
                }
                is FolderContentUiState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Initializing...")
                    }
                }
            }
            // General loading overlay for uploads/downloads (optional, can rely on pull-to-refresh indicator too)
            if (fileUploadState is FileUploadUiState.Loading ||
                fileDownloadState is FileDownloadUiState.Loading || fileDownloadState is FileDownloadUiState.Downloading ||
                filePublicDownloadState is FilePublicDownloadUiState.Downloading) {
                Box(modifier = Modifier.fillMaxSize().align(Alignment.Center), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.padding(32.dp),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when {
                                    fileUploadState is FileUploadUiState.Loading -> "Uploading..."
                                    fileDownloadState is FileDownloadUiState.Loading || fileDownloadState is FileDownloadUiState.Downloading -> "Opening file..."
                                    filePublicDownloadState is FilePublicDownloadUiState.Downloading -> "Downloading to public folder..."
                                    else -> "Processing..."
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddFolderDialog && isManager) {
        FolderEditDialog(
            title = "Create New Folder",
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { name, description ->
                foldersViewModel.createFolder(name, description, currentFolderId)
                showAddFolderDialog = false
            }
        )
    }

    showEditFolderDialog?.let { folderToEdit ->
        if (isManager) {
            FolderEditDialog(
                existingFolder = folderToEdit,
                title = "Edit Folder",
                onDismiss = { showEditFolderDialog = null },
                onConfirm = { name, description ->
                    foldersViewModel.updateFolder(folderToEdit.id, name, description, folderToEdit.parentId)
                    showEditFolderDialog = null
                }
            )
        }
    }
    showDeleteFolderDialog?.let { folderToDelete ->
        if (isManager) {
            AlertDialog(
                onDismissRequest = { showDeleteFolderDialog = null },
                title = { Text("Delete Folder") },
                text = { Text("Are you sure you want to delete folder \"${folderToDelete.name}\"? This action may delete all its contents (subfolders and files) depending on database setup. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            foldersViewModel.deleteFolder(folderToDelete.id, folderToDelete.parentId)
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
    }
    if (showCreateUserDialog && isManager && currentFolderId == null) { // Only at root
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { username, email, password ->
                appViewModel.adminCreateUser(username, email, password, isAdmin = false)
                // Dialog closure handled by LaunchedEffect on userCreationAlert
            }
        )
    }
}

@Composable
fun FolderListItem(
    folder: Folder,
    isManager: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = "Folder",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (folder.description.isNotBlank()) {
                    Text(folder.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (isManager) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, "Edit Folder")
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Delete Folder", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderEditDialog(
    existingFolder: Folder? = null,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember(existingFolder) { mutableStateOf(existingFolder?.name ?: "") }
    var description by remember(existingFolder) { mutableStateOf(existingFolder?.description ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isNotBlank()) null else "Folder name cannot be empty."
                    },
                    label = { Text("Folder Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true
                )
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
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
            Button(onClick = {
                if (name.isBlank()) {
                    nameError = "Folder name cannot be empty."
                } else {
                    onConfirm(name.trim(), description.trim())
                }
            }) { Text(if (existingFolder == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog( // Copied from original FoldersScreen, can be moved to a common components file
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
                usernameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
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
                emailError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = if (it.length >= 6) null else "Password must be at least 6 characters."
                    },
                    label = { Text("Password*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = passwordError != null,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, if (passwordVisible) "Hide password" else "Show password")
                        }
                    }
                )
                passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
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
                    passwordError = if (isPasswordCurrentlyValid) null else "Password must be at least 6 characters."

                    if (isUsernameValid && isEmailCurrentlyValid && isPasswordCurrentlyValid) {
                        onConfirm(username.trim(), email.trim(), password)
                    }
                }
            ) { Text("Create User") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}