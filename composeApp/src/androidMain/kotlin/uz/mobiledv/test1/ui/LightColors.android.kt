package uz.mobiledv.test1.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal actual fun PlatformSpecificSystemUiController(useDarkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Make system bars transparent for edge-to-edge
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()

                // This ensures content draws behind system bars,
                // should already be handled by enableEdgeToEdge(), but reiterating for clarity.
                WindowCompat.setDecorFitsSystemWindows(window, false)

                // Control the color of the icons in the status bar and navigation bar
                // If useDarkTheme is true, isAppearanceLightStatusBars becomes false (light icons)
                // If useDarkTheme is false, isAppearanceLightStatusBars becomes true (dark icons)
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
}