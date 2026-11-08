package no.timnor.app.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TimnorSpacing(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

internal val LocalTimnorSpacing = staticCompositionLocalOf { TimnorSpacing() }

val MaterialTheme.timnorSpacing: TimnorSpacing
    @Composable
    get() = LocalTimnorSpacing.current
