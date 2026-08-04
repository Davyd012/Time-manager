package org.examples.time_manager.ui.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.examples.time_manager.App

@Composable
fun TimeMangerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    App.hasCutOut = hasCutout()
    val current = LocalView.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.statusBarHeight == 0.dp) {
        val insets = current.rootWindowInsets?.let { insets ->
            WindowInsetsCompat.toWindowInsetsCompat(insets)
        }
        if (insets != null) {
            val density = current.resources.displayMetrics.density
            val statusBarHeightDp =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top.let { (it / density).dp }

            App.statusBarHeight = statusBarHeightDp
        }
    }

    if (!current.isInEditMode) {
        LaunchedEffect(true) {
            val window = (current.context as Activity).window
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, current)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
