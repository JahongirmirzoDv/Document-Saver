package uz.mobiledv.test1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // Keep Material3 Text
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
import uz.mobiledv.test1.components.UpdateChecker
import uz.mobiledv.test1.screens.FolderDetailScreen
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.LoginScreen
import uz.mobiledv.test1.ui.AppTheme

@Composable
@Preview
fun App() {
    val viewModel: AppViewModel = koinViewModel()

    // Apply the AppTheme at the root of your application
    AppTheme {
        val navController = rememberNavController()
        val customSessionStatus by viewModel.customSessionStatus.collectAsStateWithLifecycle()

        //UpdateChecker() // Add the UpdateChecker

        val startDestination by remember(customSessionStatus) {
            derivedStateOf {
                when (customSessionStatus) {
                    is CustomSessionStatus.Authenticated -> "folders"
                    is CustomSessionStatus.NotAuthenticated -> "login"
                    is CustomSessionStatus.Initializing -> "loading"
                }
            }
        }

        LaunchedEffect(customSessionStatus, startDestination) {
            val currentRoute = navController.currentDestination?.route
            if (startDestination == "loading" && customSessionStatus is CustomSessionStatus.Initializing) {
                if (currentRoute != "loading") {
                    // navController.navigate("loading") {
                    // popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    // launchSingleTop = true
                    // }
                }
            } else if (customSessionStatus is CustomSessionStatus.Authenticated) {
                if (currentRoute != "folders" && !currentRoute.orEmpty().startsWith("folderDetail")) {
                    navController.navigate("folders") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else if (customSessionStatus is CustomSessionStatus.NotAuthenticated) {
                if (currentRoute != "login") {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        if (startDestination == "loading" && customSessionStatus is CustomSessionStatus.Initializing) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text("Initializing session...", style = MaterialTheme.typography.bodyLarge) // Use M3 Typography
            }
        } else {
            NavHost(
                navController,
                startDestination = startDestination
            ) {
                composable("loading") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Please wait...", style = MaterialTheme.typography.bodyLarge) // Use M3 Typography
                        }
                    }
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            // Navigation handled by LaunchedEffect
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
                            viewModel.logout()
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
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
