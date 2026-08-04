package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class DayWork(
    val date: LocalDate,
    val hours: Double,
)

@Immutable
data class MonthViewColors(
    val background: androidx.compose.ui.graphics.Color,
    val backgroundGlow: androidx.compose.ui.graphics.Color,
    val cardBackground: androidx.compose.ui.graphics.Color,
    val chipBackground: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val divider: androidx.compose.ui.graphics.Color,
    val text: androidx.compose.ui.graphics.Color,
    val mutedText: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val accentMuted: androidx.compose.ui.graphics.Color,
)
