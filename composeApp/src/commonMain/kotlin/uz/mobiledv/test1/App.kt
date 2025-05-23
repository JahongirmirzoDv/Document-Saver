package uz.mobiledv.test1

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import uz.mobiledv.test1.screens.LoginScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    MaterialTheme {
        val navController = rememberNavController()

        val viewModel: AppViewModel = koinViewModel()

        val loginAlert by viewModel.loginAlert.collectAsStateWithLifecycle()
        val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()
        println(sessionStatus)

        var route by remember { mutableStateOf("login") }

        route = when(sessionStatus) {
            is SessionStatus.Authenticated -> {
                "folders"
            }

            is SessionStatus.NotAuthenticated -> {
                "login"
            }

            SessionStatus.Initializing -> TODO()
            is SessionStatus.RefreshFailure -> TODO()
        }


        NavHost(navController, startDestination = route) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { user ->
                        navController.navigate("folders") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            composable("folders") {
                Box(modifier = Modifier.fillMaxSize(),contentAlignment = Alignment.Center){
                    Text("Folders Screen")
                }
            }
        }
    }
}