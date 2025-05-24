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
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.data.AuthSettings


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Callback for successful login
    appViewModel: AppViewModel = koinViewModel(),
    authSettings: AuthSettings = koinInject() // Inject AuthSettings
) {
    var email by remember { mutableStateOf(authSettings.getLastLoggedInEmail() ?: "") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) } // For client-side validation errors
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(authSettings.getLastLoggedInEmail() != null) }

    val loginAlert by appViewModel.loginAlert.collectAsStateWithLifecycle()
    val sessionStatus by appViewModel.sessionStatus.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    // Handles Snackbar messages from the ViewModel (e.g., login success/failure)
    LaunchedEffect(loginAlert) {
        loginAlert?.let {
            snackbarHostState.showSnackbar(it)
            isLoading = false // Stop loading when a message (error or info) is shown
            appViewModel.loginAlert.value = null // Clear the alert
        }
    }


    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> {
                isLoading = false // Should be false if authenticated
                // Navigation is typically handled in App.kt based on sessionStatus
            }
            is SessionStatus.NotAuthenticated -> {
                // If a login attempt failed and resulted in NotAuthenticated,
                // and loginAlert was not set (e.g. some other auth flow), ensure isLoading is false.
                // However, loginAlert effect is the primary way to stop isLoading on API errors.
                if (isLoading) { // Only set isLoading to false if it was true
                    // This case might be redundant if loginAlert handles all error scenarios
                }
            }
            is SessionStatus.Initializing -> {
                // This state might be set by Supabase during session refresh, not directly by our login button
                // isLoading = true // Uncomment if you want to show loading for this state too
            }
            else -> { // Covers MfaRequired, RefreshFailure, Initializing
                isLoading = false // Ensure loading stops for other terminal/intermediate states
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
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null // Clear client-side error when user types
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                isError = error != null && (error?.contains("email", ignoreCase = true) == true || error?.contains("credentials", ignoreCase = true) == true),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null // Clear client-side error when user types
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
                isError = error != null && (error?.contains("password", ignoreCase = true) == true || error?.contains("credentials", ignoreCase = true) == true),
                singleLine = true
            )

            // Display client-side validation errors
            error?.let { currentError ->
                if (!currentError.contains("credentials", ignoreCase = true)) { // Only show if not a server error
                    Text(
                        currentError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = {
                        rememberMe = it
                        if (!it) {
                            authSettings.saveLastLoggedInEmail(null)
                        } else if (email.isNotBlank()) {
                            authSettings.saveLastLoggedInEmail(email)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember Email")
            }

            Button(
                onClick = {
                    var localValidationError = ""
                    if (email.isBlank()) {
                        localValidationError += "Email cannot be empty.\n"
                    }
                    if (password.isBlank()) {
                        localValidationError += "Password cannot be empty."
                    }

                    if (localValidationError.isNotBlank()) {
                        error = localValidationError.trim()
                        return@Button
                    }

                    isLoading = true
                    error = null // Clear previous client-side errors before login attempt
                    if (rememberMe) {
                        authSettings.saveLastLoggedInEmail(email)
                    } else {
                        authSettings.saveLastLoggedInEmail(null)
                    }
                    appViewModel.login(email, password, rememberMe)
                },
                enabled = !isLoading, // Button is enabled if not loading
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
            TextButton(
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Sign up not implemented yet.") }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}

// @Preview
// @Composable
// fun PreviewLoginScreen() {
//    MaterialTheme {
//        LoginScreen(onLoginSuccess = {}, appViewModel = koinViewModel(), authSettings = koinViewModel())
//    }
// }