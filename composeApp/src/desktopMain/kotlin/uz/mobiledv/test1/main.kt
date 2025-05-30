package uz.mobiledv.test1

import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getResourceUri
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform
import test1.composeapp.generated.resources.Res
import test1.composeapp.generated.resources.allDrawableResources
import uz.mobiledv.test1.di.initKoin
import uz.mobiledv.test1.util.FileSaver

fun main() = application {
    initKoin()

    val koin = KoinPlatform.getKoin()
//    val icon = painterResource(DrawableResource(Res.allDrawableResources., "png_icon.png"))

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
        state = WindowState(placement = WindowPlacement.Maximized),
    ) {
        App()
    }
}