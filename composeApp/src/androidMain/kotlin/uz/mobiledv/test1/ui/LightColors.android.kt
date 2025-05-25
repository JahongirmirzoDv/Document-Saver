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
    val color = MaterialTheme.colorScheme.background.toArgb()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set status and navigation bar to be transparent
                // This is often handled by enableEdgeToEdge() in the Activity,
                // but reinforcing it here or matching to background can be useful.
                window.statusBarColor = color
                window.navigationBarColor = color


                // This controls the color of the icons in the status bar and navigation bar
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
}