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
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.realtime.Column
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.screens.FolderDetailScreen
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.LoginScreen
import uz.mobiledv.test1.screens.SimpleViewModel

@Composable
@Preview
fun App() {
    val viewModel: AppViewModel = koinViewModel()
    MaterialTheme {
        val navController = rememberNavController()
        val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()

        val startDestination by remember(sessionStatus) {
            derivedStateOf { // Use derivedStateOf for cleaner state observation
                when (sessionStatus) {
                    is SessionStatus.Authenticated -> "folders"
                    is SessionStatus.NotAuthenticated -> "login"
                    else -> "loading" // Covers RefreshFailure, MfaRequired, etc.
                }
            }
        }

        LaunchedEffect(sessionStatus) {
            if (sessionStatus is SessionStatus.NotAuthenticated) {
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            } else if (sessionStatus is SessionStatus.Authenticated) {
                if (navController.currentDestination?.route != "folders" &&
                    !navController.currentDestination?.route.orEmpty().startsWith("folderDetail")
                ) {
                    navController.navigate("folders") {
                        popUpTo("login") { inclusive = true } // Clear login from backstack
                        launchSingleTop = true
                    }
                }
            }
        }


        if (startDestination == "loading" && sessionStatus !is SessionStatus.Authenticated && sessionStatus !is SessionStatus.NotAuthenticated) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    when (sessionStatus) {
                        is SessionStatus.Initializing -> "Checking session..."
                        is SessionStatus.RefreshFailure -> "Session refresh failed. Check connection."
                        else -> "Initializing..."
                    }
                )
            }
        } else {
            NavHost(navController, startDestination = startDestination) {
                composable("loading") { // Keep a loading route for explicit navigation if needed
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Please wait...")
                    }
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("folders") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        appViewModel = viewModel
                    )
                }
                composable("folders") {
                    FoldersScreen(
                        appViewModel = viewModel,
                        onFolderClick = { folder ->
                            navController.navigate("folderDetail/${folder.id}/${folder.name}")
                        },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        navController = navController
                    )
                }
                composable(
                    route = "folderDetail/{folderId}/{folderName}",
                    arguments = listOf(
                        navArgument("folderId") { type = NavType.StringType },
                        navArgument("folderName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val folderId = backStackEntry.arguments?.getString("folderId")
                    val folderName = backStackEntry.arguments?.getString("folderName")
                    if (folderId != null && folderName != null) {
                        FolderDetailScreen(
                            folderId = folderId,
                            folderName = folderName,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    } else {
                        // Handle error or navigate back if arguments are missing
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}