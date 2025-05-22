package uz.mobiledv.test1

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import uz.mobiledv.test1.di.initKoin

fun main() = application {
    initKoin()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Test1",
    ) {
        App()
    }
}