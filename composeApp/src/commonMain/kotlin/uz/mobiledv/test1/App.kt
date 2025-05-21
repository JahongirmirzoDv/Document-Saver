package uz.mobiledv.test1

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.gitlive.firebase.firestore.FirebaseFirestore
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import test1.composeapp.generated.resources.Res
import test1.composeapp.generated.resources.compose_multiplatform
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.repository.DocumentRepositoryImpl
import uz.mobiledv.test1.repository.FolderRepositoryImpl
import uz.mobiledv.test1.repository.UserRepositoryImpl
import uz.mobiledv.test1.screens.LoginScreen
import uz.mobiledv.test1.screens.FoldersScreen
import uz.mobiledv.test1.screens.DocumentsScreen
import uz.mobiledv.test1.screens.UserManagementScreen

@Composable
@Preview
fun App() {

    MaterialTheme {
        val navController = rememberNavController()
        val userRepository = remember { UserRepositoryImpl() }
        val folderRepository = remember { FolderRepositoryImpl() }
        val documentRepository = remember { DocumentRepositoryImpl() }
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
                    userRepository = userRepository
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
                    folderRepository = folderRepository
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
                    documentRepository = documentRepository
                )
            }
            composable("user_management") {
                UserManagementScreen(
                    onBackClick = {
                        navController.navigateUp()
                    },
                    userRepository = userRepository
                )
            }
        }
    }
}