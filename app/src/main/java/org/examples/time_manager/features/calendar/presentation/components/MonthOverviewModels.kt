package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

@Immutable
data class DayWork(
    val date: LocalDate,
    val hours: Double,
)

@Immutable
data class MonthViewColors(
    val background: Color,
    val backgroundGlow: Color,
    val cardBackground: Color,
    val chipBackground: Color,
    val border: Color,
    val divider: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
    val accentMuted: Color,
) {
    companion object {
        fun defaults(): MonthViewColors =
            MonthViewColors(
                background = Color(0xFF071111),
                backgroundGlow = Color(0xFF172426),
                cardBackground = Color(0xFF1A2628).copy(alpha = 0.92f),
                chipBackground = Color(0xFF233234).copy(alpha = 0.9f),
                border = Color(0xFFD7F6F2).copy(alpha = 0.16f),
                divider = Color(0xFFD7F6F2).copy(alpha = 0.10f),
                text = Color(0xFFF4F7F6),
                mutedText = Color(0xFFC2C9C9),
                accent = Color(0xFF83EC89),
                accentMuted = Color(0xFF83EC89).copy(alpha = 0.65f),
            )
    }
}
