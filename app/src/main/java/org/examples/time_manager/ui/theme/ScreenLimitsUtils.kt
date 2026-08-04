package org.examples.time_manager.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

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