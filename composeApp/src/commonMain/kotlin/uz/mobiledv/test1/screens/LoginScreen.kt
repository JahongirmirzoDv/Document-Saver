// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/LoginScreen.kt
package uz.mobiledv.test1.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// import io.github.jan.supabase.auth.status.SessionStatus // Not needed anymore
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.CustomSessionStatus
import uz.mobiledv.test1.data.AuthSettings
import uz.mobiledv.test1.util.isValidEmail


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Callback for successful login
    appViewModel: AppViewModel = koinViewModel(),
    authSettings: AuthSettings = koinInject()
) {
    // Use email or username for login
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
            appViewModel.operationAlert.value = null // Clear the alert
        }
    }

    LaunchedEffect(customSessionStatus) {
        when (customSessionStatus) {
            is CustomSessionStatus.Authenticated -> {
                isLoading = false
                onLoginSuccess() // Trigger navigation
            }
            is CustomSessionStatus.NotAuthenticated -> {
                isLoading = false // Stop loading if auth fails or on initial load
            }
            is CustomSessionStatus.Initializing -> {
                isLoading = true // Show loading indicator
            }
        }
    }


    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Login / Sign Up",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    clientError = null
                },
                label = { Text("Username or Email") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                isError = clientError != null && clientError?.contains("identifier", ignoreCase = true) == true,
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    clientError = null
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                isError = clientError != null && clientError?.contains("password", ignoreCase = true) == true,
                singleLine = true
            )

            clientError?.let { currentError ->
                Text(
                    currentError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = {
                        rememberMe = it
                        // Logic to save/clear email preference moved to login action
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember me")
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
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Login")
                }
            }
            TextButton( // Example: This could navigate to a separate sign-up screen or show a sign-up dialog
                onClick = {
                    // For simplicity, we'll use the adminCreateUser for "Sign Up" from a non-admin context.
                    // In a real app, you'd have a separate sign-up flow.
                    // This example assumes 'identifier' is email for sign-up.
                    // You'd likely have separate fields for username, email, password in a real sign-up form.
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
                    // Non-admin users creating their own accounts. isAdmin will be false.
                    // The `adminCreateUser` in AppViewModel could be split or adapted
                    // if you need distinct sign-up vs admin-creation logic.
                    // For now, we'll use a simplified approach:
                    scope.launch {
                        snackbarHostState.showSnackbar("Sign-up: Desktop admin needs to create your account, or this feature needs full implementation with a dedicated sign-up screen/flow.")
                        // If you want users to self-register:
                        // appViewModel.selfRegisterUser(identifier, password) // You'd need to create this method
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Don't have an account? Contact Admin / Sign Up (Basic)")
            }
        }
    }
}