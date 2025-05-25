// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/App.kt
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
import uz.mobiledv.test1.screens.FolderDetailScreen
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.LoginScreen
// import uz.mobiledv.test1.util.PlatformType // No longer needed for startDestination decision based on platform distinction for visibility
// import uz.mobiledv.test1.util.getCurrentPlatform // No longer needed for startDestination decision

@Composable
@Preview
fun App() {
    val viewModel: AppViewModel = koinViewModel()
    MaterialTheme {
        val navController = rememberNavController()
        val customSessionStatus by viewModel.customSessionStatus.collectAsStateWithLifecycle()

        // val currentPlatform = remember { getCurrentPlatform() } // Retain if isManager distinction for actions is still platform-dependent
        // val isManager = viewModel.isManager // Used to control UI elements for actions

        // Simplified start destination: depends only on authentication status.
        val startDestination by remember(customSessionStatus) {
            derivedStateOf {
                when (customSessionStatus) {
                    is CustomSessionStatus.Authenticated -> "folders"
                    is CustomSessionStatus.NotAuthenticated -> "login"
                    is CustomSessionStatus.Initializing -> "loading" // Default to loading while checking auth
                }
            }
        }

        // Simplified navigation effect: navigates based on auth status.
        LaunchedEffect(customSessionStatus, startDestination) {
            val currentRoute = navController.currentDestination?.route
            if (startDestination == "loading" && customSessionStatus is CustomSessionStatus.Initializing) {
                // If still initializing and not on loading screen, navigate there.
                if (currentRoute != "loading") {
                    // navController.navigate("loading") {
                    //    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    //    launchSingleTop = true
                    // }
                }
            } else if (customSessionStatus is CustomSessionStatus.Authenticated) {
                if (currentRoute != "folders" && !currentRoute.orEmpty().startsWith("folderDetail")) {
                    navController.navigate("folders") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true } // Pop up to the graph's start (e.g. loading or login)
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


        // Handle the "loading" state explicitly before NavHost if it's the startDestination
        // and the session is still initializing.
        if (startDestination == "loading" && customSessionStatus is CustomSessionStatus.Initializing) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text("Initializing session...")
            }
        } else {
            // Ensure NavHost uses a start destination that won't cause immediate re-navigation loops
            // If startDestination is 'loading' but status is no longer Initializing, NavHost will handle it.
            NavHost(
                navController,
                startDestination = startDestination
            ) {
                composable("loading") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Please wait...")
                        }
                    }
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            // Navigation is now primarily handled by the LaunchedEffect observing customSessionStatus
                            // Navigating here explicitly might conflict, let the effect handle it.
                            // if (navController.currentDestination?.route != "folders") {
                            // navController.navigate("folders") {
                            // popUpTo("login") { inclusive = true }
                            // launchSingleTop = true
                            // }
                            // }
                        },
                        appViewModel = viewModel
                    )
                }
                composable("folders") {
                    FoldersScreen(
                        appViewModel = viewModel, // Pass AppViewModel for manager checks and user creation
                        onFolderClick = { folder ->
                            navController.navigate("folderDetail/${folder.id}/${folder.name}")
                        },
                        onLogout = {
                            viewModel.logout()
                            // Navigation to login will be handled by LaunchedEffect
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
                        // Should not happen if navigation is correct, but as a fallback:
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}