package uz.mobiledv.test1

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jan.supabase.auth.status.SessionStatus
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.LoginScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    MaterialTheme {
        val navController = rememberNavController()
        val viewModel: AppViewModel = koinViewModel()

        val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()
        println(sessionStatus)

        val startDestination = remember(sessionStatus) {
            when (sessionStatus) {
                is SessionStatus.Authenticated -> "folders"
                is SessionStatus.NotAuthenticated -> "login"
                is SessionStatus.RefreshFailure -> "loading" // Handle loading/error state
                else -> "loading" // Default to loading, covers other cases like MFA
            }
        }

        if (startDestination == "loading") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator() // Show a loading indicator
                Text("Checking session...")
            }
        } else {
            NavHost(navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen()
                }
                composable("folders") {
                    FoldersScreen()
                }
            }
        }
    }
}