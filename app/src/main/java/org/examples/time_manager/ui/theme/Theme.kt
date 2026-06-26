package org.examples.time_manager.ui.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.examples.time_manager.App

@Composable
fun TimeMangerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(true) {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun hasCutout(): Boolean {
    val view = LocalView.current
    var hasCutout = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val windowInsets = ViewCompat.getRootWindowInsets(view)
        hasCutout = windowInsets?.displayCutout != null
    }

    return hasCutout
}
