package org.examples.time_manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val small: androidx.compose.ui.unit.Dp = 8.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val extraLarge: androidx.compose.ui.unit.Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val MaterialTheme.spacing: Spacing
    @androidx.compose.runtime.Composable
    get() = LocalSpacing.current
