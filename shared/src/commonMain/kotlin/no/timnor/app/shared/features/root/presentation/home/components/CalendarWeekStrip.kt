package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
internal fun CalendarWeekStrip(
    weekDays: List<LocalDate>,
    weekdays: List<String>,
    secondsByDay: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    expansionProgress: Float,
    expanded: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToggleExpanded: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
        ) {
            weekDays.forEach { date ->
                CalendarDayCell(
                    date = date,
                    dayLabel = weekdays[date.dayOfWeek.ordinal],
                    seconds = (secondsByDay[date] ?: 0).coerceAtMost(Int.MAX_VALUE),
                    selected = date == selectedDate,
                    enabled = expansionProgress <= 0f,
                    onClick = { onSelectDate(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        onToggleExpanded?.let { toggle ->
            HomeCalendarToggle(
                expanded = expanded,
                onToggle = toggle,
                enabled = expansionProgress <= 0f,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
