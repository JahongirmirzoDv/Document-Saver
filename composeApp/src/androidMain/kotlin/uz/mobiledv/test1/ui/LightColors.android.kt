package uz.mobiledv.test1.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal actual fun PlatformSpecificSystemUiController(useDarkTheme: Boolean) {
    val view = LocalView.current
    // It's generally better to use a color that contrasts well with your content,
    // or a transparent color if enableEdgeToEdge handles it.
    // Using MaterialTheme.colorScheme.background might be fine, but sometimes
    // surface or a specific scrim color is preferred.
    val systemBarColor = MaterialTheme.colorScheme.background.toArgb() // Or another color like Surface

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set status and navigation bar colors
                // If enableEdgeToEdge is working correctly, these might not strictly be needed
                // or should be transparent if you want content to draw behind them.
                // Consider making them transparent if enableEdgeToEdge is the primary mechanism.
                window.statusBarColor = systemBarColor // or Color.Transparent.toArgb()
                window.navigationBarColor = systemBarColor // or Color.Transparent.toArgb()

                // This controls the color of the icons in the status bar and navigation bar
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
}