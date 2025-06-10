package uz.mobiledv.test1

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import uz.mobiledv.test1.di.initKoin

fun MainViewController(): UIViewController {
    // Initialize Koin for iOS
    initKoin {
        // any additional iOS-specific Koin configuration
    }
    return ComposeUIViewController { App() }
}