package uz.mobiledv.test1

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import uz.mobiledv.test1.repository.FirebaseInitializer

fun main() = application {
    FirebaseInitializer.initialize()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Test1",
    ) {
        App()
    }
}