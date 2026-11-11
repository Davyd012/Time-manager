package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
            modifier = Modifier.fillMaxWidth()
                .testTag("home-week-strip")
                .padding(top = MaterialTheme.timnorSpacing.extraSmall / 2),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
        ) {
            weekDays.forEach { date ->
                CalendarDayCell(
                    date = date,
                    dayLabel = legacyHomeWeekdayLabel(
                        date = date,
                        selectedDate = selectedDate,
                        localizedWeekday = weekdays[date.dayOfWeek.ordinal],
                    ),
                    seconds = (secondsByDay[date] ?: 0).coerceAtMost(Int.MAX_VALUE),
                    selected = date == selectedDate,
                    enabled = isHomeWeekRowInteractive(expansionProgress) &&
                        date.year == selectedDate.year && date.month == selectedDate.month,
                    showHours = date.year == selectedDate.year && date.month == selectedDate.month,
                    onClick = { onSelectDate(date) },
                    modifier = Modifier.weight(1f).testTag("calendar-week-day-${date.year}-${date.month.ordinal + 1}-${date.day}"),
                )
            }
        }
        onToggleExpanded?.let { toggle ->
            HomeCalendarToggle(
                expanded = expanded,
                onToggle = toggle,
                enabled = isHomeWeekRowInteractive(expansionProgress),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(
                    top = MaterialTheme.timnorSpacing.small + MaterialTheme.timnorSpacing.extraSmall / 2,
                    bottom = MaterialTheme.timnorSpacing.small + MaterialTheme.timnorSpacing.extraSmall / 2,
                ),
            )
        }
    }
}
