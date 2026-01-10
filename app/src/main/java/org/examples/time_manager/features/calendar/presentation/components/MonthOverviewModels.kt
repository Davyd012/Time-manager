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
    val cardBackground: Color,
    val border: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
) {
    companion object {
        fun defaults(): MonthViewColors =
            MonthViewColors(
                background = Color(0xFF0F0F10),
                cardBackground = Color(0xFF1A1B1E),
                border = Color.White.copy(alpha = 0.1f),
                text = Color(0xFFEDEDED),
                mutedText = Color(0xFFEDEDED).copy(alpha = 0.6f),
                accent = Color(0xFFF3A43B),
            )
    }
}
