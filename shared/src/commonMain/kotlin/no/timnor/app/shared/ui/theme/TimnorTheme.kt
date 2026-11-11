package no.timnor.app.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1C1C1B),
    secondary = Color(0xFFCEC6B2),
    tertiary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF343022),
    onTertiary = Color(0xFF2C331A),
    primaryContainer = Color(0xFFE1D4AA),
    secondaryContainer = Color(0xFF413D2E),
    tertiaryContainer = Color(0xFFD0D9B4),
    onPrimaryContainer = Color(0xFF473F20),
    onSecondaryContainer = Color(0xFFD8D0BC),
    onTertiaryContainer = Color(0xFF3A4228),
    surfaceDim = Color(0xFF141310),
    surface = Color(0xFF161615),
    surfaceBright = Color(0xFF3B3936),
    surfaceContainerLowest = Color(0xFF0F0E0B),
    surfaceContainerLow = Color(0xFF1D1B18),
    surfaceContainer = Color(0xFF2B2C34),
    surfaceContainerHigh = Color(0xFF2B2A27),
    surfaceContainerHighest = Color(0xFF363431),
    onSurface = Color(0xFFE7E2DD),
    onSurfaceVariant = Color(0xFFCCC6B8),
    outline = Color(0xFF969084),
    outlineVariant = Color(0xFF4A473C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE7E2DD),
    inverseOnSurface = Color(0xFF32302D),
    inversePrimary = Color(0xFF675E3C),
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF675E3C),
    secondary = Color(0xFF635E4E),
    tertiary = Color(0xFF596244),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFF2CB),
    secondaryContainer = Color(0xFFC9BEA1),
    tertiaryContainer = Color(0xFFEEF7D1),
    onPrimaryContainer = Color(0xFF1D1D1D),
    onSecondaryContainer = Color(0xFF573333),
    onTertiaryContainer = Color(0xFF5A5050),
    surfaceDim = Color(0xFFE7E2DD),
    surface = Color(0xFFE7E2DD),
    surfaceBright = Color(0xFFE7E2DD),
    surfaceContainerLowest = Color(0xFFE7E2DD),
    surfaceContainerLow = Color(0xFFE7E2DD),
    surfaceContainer = Color(0xFFE7E2DD),
    surfaceContainerHigh = Color(0xFFE7E2DD),
    surfaceContainerHighest = Color(0xFFE7E2DD),
    onSurface = Color(0xFF1D1B18),
    onSurfaceVariant = Color(0xFF4A473C),
    outline = Color(0xFF7B776B),
    outlineVariant = Color(0xFFCCC6B8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF32302D),
    inverseOnSurface = Color(0xFFF5F0EB),
    inversePrimary = Color(0xFFD3C69D),
    scrim = Color(0xFF000000),
)

private val TimnorTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)

/** Shared Material 3 token provider; palette and body typography mirror the Android theme. */
@Composable
fun TimnorTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTimnorSpacing provides TimnorSpacing()) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = TimnorTypography,
            content = content,
        )
    }
}
