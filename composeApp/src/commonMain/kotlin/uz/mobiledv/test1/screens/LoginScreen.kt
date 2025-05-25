package uz.mobiledv.test1.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.CustomSessionStatus
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.util.isValidEmail

@OptIn(ExperimentalMaterial3Api::class) // Ensure this is present for Scaffold, etc.
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    appViewModel: AppViewModel = koinViewModel(),
    authSettings: AuthSettings = koinInject()
) {
    var identifier by remember { mutableStateOf(authSettings.getLastLoggedInEmail() ?: "") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var clientError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(authSettings.getLastLoggedInEmail() != null) }

    val operationAlert by appViewModel.operationAlert.collectAsStateWithLifecycle()
    val customSessionStatus by appViewModel.customSessionStatus.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationAlert) {
        operationAlert?.let {
            snackbarHostState.showSnackbar(it)
            appViewModel.operationAlert.value = null
        }
    }

    LaunchedEffect(customSessionStatus) {
        when (customSessionStatus) {
            is CustomSessionStatus.Authenticated -> {
                isLoading = false
                onLoginSuccess()
            }
            is CustomSessionStatus.NotAuthenticated -> {
                isLoading = false
            }
            is CustomSessionStatus.Initializing -> {
                isLoading = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background // Use theme background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Login / Sign Up",
                style = MaterialTheme.typography.headlineSmall, // Use M3 Typography
                color = MaterialTheme.colorScheme.onBackground, // Use M3 Color
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    clientError = null
                },
                label = { Text("Username or Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                isError = clientError != null && clientError?.contains("identifier", ignoreCase = true) == true,
                singleLine = true,
//                colors = TextFieldDefaults.colors( // M3 Text field colors
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                    textColor = MaterialTheme.colorScheme.onSurface,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    clientError = null
                },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = description,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // M3 Color
                        )
                    }
                },
                isError = clientError != null && clientError?.contains("password", ignoreCase = true) == true,
                singleLine = true,
//                colors = TextFieldDefaults.colors( // M3 Text field colors
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                    textColor = MaterialTheme.colorScheme.onSurface,
//                    cursorColor = MaterialTheme.colorScheme.primary
//                )
            )

            clientError?.let { currentError ->
                Text(
                    currentError,
                    color = MaterialTheme.colorScheme.error, // M3 Color
                    style = MaterialTheme.typography.bodySmall, // M3 Typography
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = {
                        rememberMe = it
                    },
                    colors = CheckboxDefaults.colors( // M3 Checkbox colors
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember me", color = MaterialTheme.colorScheme.onBackground) // M3 Color
            }

            Button(
                onClick = {
                    var localValidationError = ""
                    if (identifier.isBlank()) {
                        localValidationError += "Username/Email cannot be empty.\n"
                    }
                    if (password.isBlank()) {
                        localValidationError += "Password cannot be empty."
                    }

                    if (localValidationError.isNotBlank()) {
                        clientError = localValidationError.trim()
                        return@Button
                    }
                    clientError = null
                    appViewModel.login(identifier, password, rememberMe)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors( // M3 Button colors
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary, // M3 Color
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp // Thinner stroke for M3 style
                    )
                } else {
                    Text("Login")
                }
            }
            TextButton(
                onClick = {
                    if (identifier.isBlank() || password.isBlank()) {
                        clientError = "Email and password are required for sign-up."
                        return@TextButton
                    }
                    if (!isValidEmail(identifier)) {
                        clientError = "Please enter a valid email to sign up."
                        return@TextButton
                    }
                    if (password.length < 6) {
                        clientError = "Password must be at least 6 characters for sign up."
                        return@TextButton
                    }
                    clientError = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Sign-up: Desktop admin needs to create your account, or this feature needs full implementation with a dedicated sign-up screen/flow.")
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary) // M3 Color
            ) {
                Text("Don't have an account? Contact Admin / Sign Up (Basic)")
            }
        }
    }
}
