package uz.mobiledv.test1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
// import uz.mobiledv.test1.components.UpdateChecker // If you have this component
import uz.mobiledv.test1.screens.FolderContentsScreen // Import the new screen
import uz.mobiledv.test1.screens.LoginScreen
import uz.mobiledv.test1.screens.decodeNavArg
import uz.mobiledv.test1.screens.encodeNavArg
import uz.mobiledv.test1.ui.AppTheme

@Composable
@Preview
fun App() {
    val appViewModel: AppViewModel = koinViewModel()

    AppTheme {
        val navController = rememberNavController()
        val customSessionStatus by appViewModel.customSessionStatus.collectAsStateWithLifecycle()

        LaunchedEffect(customSessionStatus, navController) {
            when (customSessionStatus) {
                is CustomSessionStatus.Authenticated -> {
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute?.startsWith("folderContents") != true) {
                        navController.navigate("folderContents/${encodeNavArg(null)}/${encodeNavArg(null)}") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                is CustomSessionStatus.NotAuthenticated -> {
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute != "login") {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                is CustomSessionStatus.Initializing -> {
                    // UI shows loading state
                }
            }
        }

        when (customSessionStatus) {
            is CustomSessionStatus.Initializing -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Initializing session...", style = MaterialTheme.typography.bodyLarge)
                }
            }
            is CustomSessionStatus.Authenticated, is CustomSessionStatus.NotAuthenticated -> {
                val startDestination = when (customSessionStatus) {
                    is CustomSessionStatus.Authenticated -> "folderContents/${encodeNavArg(null)}/${encodeNavArg(null)}"
                    is CustomSessionStatus.NotAuthenticated -> "login"
                    else -> "login" // Fallback for safety, though Initializing is handled
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { /* Auth status change triggers LaunchedEffect */ },
                            appViewModel = appViewModel
                        )
                    }

                    composable(
                        route = "folderContents/{encodedFolderId}/{encodedFolderName}",
                        arguments = listOf(
                            navArgument("encodedFolderId") {
                                type = NavType.StringType
                                nullable = true // Allow null from NavComponent's perspective
                            },
                            navArgument("encodedFolderName") {
                                type = NavType.StringType
                                nullable = true // Allow null from NavComponent's perspective
                            }
                        )
                    ) { backStackEntry ->
                        // getString can return null if the argument is not present or if it's explicitly null.
                        // Our path always provides a string ("value" or "null").
                        val encodedFolderIdFromArgs = backStackEntry.arguments?.getString("encodedFolderId")
                        val encodedFolderNameFromArgs = backStackEntry.arguments?.getString("encodedFolderName")

                        val folderId = decodeNavArg(encodedFolderIdFromArgs) // Handles "null" string to String?
                        val folderName = decodeNavArg(encodedFolderNameFromArgs) // Handles "null" string to String?

                        FolderContentsScreen(
                            navController = navController,
                            currentFolderId = folderId,
                            currentFolderName = folderName,
                            appViewModel = appViewModel
                        )
                    }
                }
            }
        }
    }
}