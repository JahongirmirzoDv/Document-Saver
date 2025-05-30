// File: jahongirmirzodv/test.1.2/Test.1.2-fcc101c924a3dcb58258c4f63c298289470731ad/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/LoginScreen.kt
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
// import uz.mobiledv.test1.util.isValidEmail // Keep if using for sign-up validation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    appViewModel: AppViewModel = koinViewModel(),
    authSettings: AuthSettings = koinInject()
) {
    var identifier by remember { mutableStateOf(authSettings.getLastLoggedInEmail() ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(authSettings.getLastLoggedInEmail() != null) }

    // State for granular error messages
    var identifierError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    // var generalError by remember { mutableStateOf<String?>(null) } // For errors not tied to a field

    val customSessionStatus by appViewModel.customSessionStatus.collectAsStateWithLifecycle()
    val operationAlert by appViewModel.operationAlert.collectAsStateWithLifecycle()

    val isLoading = customSessionStatus is CustomSessionStatus.Initializing

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationAlert) {
        operationAlert?.let { message ->
            snackbarHostState.showSnackbar(message)
            appViewModel.operationAlert.value = null // Consume alert
        }
    }

    LaunchedEffect(customSessionStatus) {
        if (customSessionStatus is CustomSessionStatus.Authenticated) {
            onLoginSuccess()
        }
        // If CustomSessionStatus.NotAuthenticated is set by ViewModel after a failed login attempt,
        // the operationAlert (snackbar) will show the error message.
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp), // Adjusted padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.headlineMedium, // Slightly larger
                color = MaterialTheme.colorScheme.primary, // Use primary color for title
                modifier = Modifier.padding(bottom = 48.dp) // Increased bottom padding
            )

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    identifierError = null // Clear error on change
                },
                label = { Text("Email") }, // Assuming login via email
                modifier = Modifier.fillMaxWidth(),
                isError = identifierError != null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium // Rounded corners
            )
            identifierError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(if (identifierError != null) 8.dp else 16.dp))


            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null // Clear error on change
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                isError = passwordError != null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium // Rounded corners
            )
            passwordError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(if (passwordError != null) 16.dp else 24.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Align checkbox and forgot password
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remember me", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                }
                // TextButton(onClick = { /* TODO: Implement Forgot Password */ }) {
                //     Text("Forgot Password?", style = MaterialTheme.typography.bodyMedium)
                // }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    var isValid = true
                    if (identifier.isBlank()) {
                        identifierError = "Email cannot be empty."
                        isValid = false
                    } else if (!identifier.contains("@") || !identifier.contains(".")) { // Basic check
                        identifierError = "Invalid email format."
                        isValid = false
                    }

                    if (password.isBlank()) {
                        passwordError = "Password cannot be empty."
                        isValid = false
                    }

                    if (isValid) {
                        appViewModel.login(identifier.trim(), password, rememberMe)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp), // Slightly taller button
                shape = MaterialTheme.shapes.medium // Rounded corners
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text("Login", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Admin creates user accounts. Please contact your administrator for access."
                        )
                    }
                },
            ) {
                Text("Don't have an account? Contact Admin", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}