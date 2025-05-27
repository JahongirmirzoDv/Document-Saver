package uz.mobiledv.test1

import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getResourceUri
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform
import test1.composeapp.generated.resources.Res
import uz.mobiledv.test1.di.initKoin
import uz.mobiledv.test1.util.FileSaver

fun main() = application {
    initKoin()

    val koin = KoinPlatform.getKoin()
//    val icon = useResource("drawable/my_window_icon.png") { loadImageBitmap(it) }

    try {
        val fileSaverInstance = koin.get<FileSaver>() // Attempt to get FileSaver
        println("SUCCESS: FileSaver instance resolved: $fileSaverInstance")
    } catch (e: Exception) {
        println("KOIN ERROR: Could not resolve FileSaver directly.")
        e.printStackTrace() // Print the full stack trace for this attempt
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Doc Saver",
    ) {
        App()
    }
}