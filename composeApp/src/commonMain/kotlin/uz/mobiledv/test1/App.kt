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
// import io.github.jan.supabase.auth.status.SessionStatus // Not needed
// import io.github.jan.supabase.realtime.Messages.Message.Companion.serializer // check if this import is actually used
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
// import uz.mobiledv.test1.model.Document // Not directly used here
import uz.mobiledv.test1.screens.FolderDetailScreen
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.LoginScreen
// import uz.mobiledv.test1.screens.SimpleViewModel // Not directly used here
import uz.mobiledv.test1.util.PlatformType
import uz.mobiledv.test1.util.getCurrentPlatform

@Composable
@Preview
fun App() {
    val viewModel: AppViewModel = koinViewModel()
    MaterialTheme {
        val navController = rememberNavController()
        val customSessionStatus by viewModel.customSessionStatus.collectAsStateWithLifecycle()

        val currentPlatform = remember { getCurrentPlatform() }
        // val isManager = viewModel.isManager // Get isManager status from AppViewModel

        val startDestination by remember(customSessionStatus, viewModel.isManager) {
            derivedStateOf {
                if (viewModel.isManager && currentPlatform == PlatformType.DESKTOP) { // Admin on desktop always sees folders
                    "folders"
                } else {
                    when (customSessionStatus) {
                        is CustomSessionStatus.Authenticated -> "folders"
                        is CustomSessionStatus.NotAuthenticated -> "login"
                        is CustomSessionStatus.Initializing -> "loading"
                    }
                }
            }
        }

        LaunchedEffect(customSessionStatus, viewModel.isManager) {
            val currentRoute = navController.currentDestination?.route
            if (viewModel.isManager && currentPlatform == PlatformType.DESKTOP) {
                if (currentRoute != "folders" && !currentRoute.orEmpty().startsWith("folderDetail")) {
                    navController.navigate("folders") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                when (customSessionStatus) {
                    is CustomSessionStatus.Authenticated -> {
                        if (currentRoute != "folders" && !currentRoute.orEmpty().startsWith("folderDetail")) {
                            navController.navigate("folders") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    is CustomSessionStatus.NotAuthenticated -> {
                        if (currentRoute != "login") {
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    is CustomSessionStatus.Initializing -> {
                        // Optionally navigate to loading, or let the NavHost handle it if 'loading' is the start dest
                        if (currentRoute != "loading" && startDestination == "loading") {
                            // navController.navigate("loading") {
                            //    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            //    launchSingleTop = true
                            // }
                        }
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
                Text("Initializing session...")
            }
        } else {
            NavHost(
                navController,
                startDestination = startDestination // Dynamic start destination
            ) {
                composable("loading") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Please wait...")
                    }
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            // Navigation is handled by LaunchedEffect observing customSessionStatus
                        },
                        appViewModel = viewModel
                    )
                }
                composable("folders") {
                    // Ensure AppViewModel provides the user for FoldersScreen if needed, or FoldersViewModel fetches it.
                    // val currentUser = (customSessionStatus as? CustomSessionStatus.Authenticated)?.user
                    FoldersScreen(
                        appViewModel = viewModel, // Pass AppViewModel for creating users etc.
                        onFolderClick = { folder ->
                            navController.navigate("folderDetail/${folder.id}/${folder.name}")
                        },
                        onLogout = {
                            viewModel.logout() // viewModel handles logout and session status change
                            // Navigation is handled by LaunchedEffect
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
                            // FolderDetailScreen might need AppViewModel or FoldersViewModel that has user context
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}