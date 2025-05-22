package uz.mobiledv.test1

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.DocumentRepositoryImpl
import uz.mobiledv.test1.repository.FolderRepositoryImpl
import uz.mobiledv.test1.screens.DocumentsScreen
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.LoginScreen
import uz.mobiledv.test1.screens.UserManagementScreen

@Composable
@Preview
fun App() {

    MaterialTheme {
        val navController = rememberNavController()
        var currentUser by remember { mutableStateOf<User?>(null) }

        NavHost(navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { user ->
                        currentUser = user
                        navController.navigate("folders") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            composable("folders") {
                FoldersScreen(
                    onFolderClick = { folder ->
                        navController.navigate("documents/${folder.id}")
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    currentUser = currentUser,
                    onUserManagementClick = {
                        navController.navigate("user_management")
                    },
                )
            }
            composable("documents/{folderId}") { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
                DocumentsScreen(
                    folderId = folderId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onDocumentClick = { document ->
                        // Handle document click
                    },
                )
            }
            composable("user_management") {
                UserManagementScreen(
                    onBackClick = {
                        navController.navigateUp()
                    },
                )
            }
        }
    }
}