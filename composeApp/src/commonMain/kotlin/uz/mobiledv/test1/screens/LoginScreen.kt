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
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.data.AuthSettings


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Callback for successful login
    appViewModel: AppViewModel = koinViewModel(),
    authSettings: AuthSettings = koinViewModel() // Inject AuthSettings
) {
    var email by remember { mutableStateOf(authSettings.getLastLoggedInEmail() ?: "") } // Load last email
    var password by remember { mutableStateOf("") }
    var isLoading by remember(appViewModel.sessionStatus.collectAsStateWithLifecycle().value) {
        mutableStateOf(false) // Reset loading state based on session status changes if needed
    }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(authSettings.getLastLoggedInEmail() != null) } // Init based on saved email

    val loginAlert by appViewModel.loginAlert.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(loginAlert) {
        loginAlert?.let {
            snackbarHostState.showSnackbar(it)
            appViewModel.loginAlert.value = null // Clear the alert
        }
    }
    LaunchedEffect(key1 = appViewModel.sessionStatus.collectAsStateWithLifecycle().value) {
        // This effect will re-run whenever sessionStatus changes.
        // We can use it to stop showing the loading indicator if a login attempt fails
        // or to trigger navigation if it succeeds (though navigation is handled in App.kt).
        isLoading = false // Assuming any change from a loading state means it's no longer loading here
        // if (appViewModel.sessionStatus.value is SessionStatus.Authenticated) {
        // onLoginSuccess() // Callback for navigation
        // }
    }


    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineSmall, // Updated typography
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                label = { Text("Email") }, // Changed from Username to Email
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                isError = error?.contains("email", ignoreCase = true) == true || error?.contains(
                    "credentials",
                    ignoreCase = true
                ) == true,
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
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
                isError = error?.contains("password", ignoreCase = true) == true || error?.contains(
                    "credentials",
                    ignoreCase = true
                ) == true,
                singleLine = true
            )

            if (error != null && error?.contains("credentials", ignoreCase = true) == true) {
                Text(
                    error!!,
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
                        if (!it) { // If unchecked, clear saved email
                            authSettings.saveLastLoggedInEmail(null)
                        } else if (email.isNotBlank()) { // If checked and email is present, save it
                            authSettings.saveLastLoggedInEmail(email)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember Email")
            }

            Button(
                onClick = {
                    var isValid = true
                    if (email.isBlank()) {
                        error = "Email cannot be empty"
                        isValid = false
                    }
                    if (password.isBlank()) {
                        error = (error?.plus("\nPassword cannot be empty") ?: "Password cannot be empty").trim()
                        isValid = false
                    }
                    if (!isValid) return@Button

                    isLoading = true
                    error = null
                    if (rememberMe) {
                        authSettings.saveLastLoggedInEmail(email)
                    } else {
                        authSettings.saveLastLoggedInEmail(null) // Clear if not remember me
                    }
                    appViewModel.login(email, password, rememberMe)
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
            // You might want a sign-up button/link here too
            TextButton(
                onClick = {
                    // Navigate to sign-up or show sign-up fields
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