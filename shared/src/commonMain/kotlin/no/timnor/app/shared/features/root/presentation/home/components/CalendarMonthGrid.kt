package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.domain.calendar.MonthGridLayout
import no.timnor.app.shared.domain.calendar.formatCalendarHours
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
internal fun CalendarMonthGrid(
    month: LocalDate,
    layout: MonthGridLayout,
    secondsByDay: Map<Int, Int>,
    selectedDate: LocalDate,
    weekdays: List<String>,
    interactive: Boolean,
    onSelectDate: (LocalDate) -> Unit,
) {
    val cells = layout.leadingEmptyDays + layout.daysInMonth + layout.trailingEmptyDays
    val weekCount = cells / 7
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = MaterialTheme.timnorSpacing.extraLarge)) {
                weekdays.forEachIndexed { index, weekday ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(weekday.take(3), style = MaterialTheme.typography.labelMedium)
                    }
                    if (index < 6) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            repeat(weekCount) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = MaterialTheme.timnorSpacing.extraLarge * 2),
                ) {
                    repeat(7) { weekday ->
                        val cellIndex = week * 7 + weekday
                        val day = cellIndex - layout.leadingEmptyDays + 1
                        if (day in 1..layout.daysInMonth) {
                            CalendarMonthDayCell(
                                date = LocalDate(month.year, month.month.ordinal + 1, day),
                                seconds = secondsByDay[day] ?: 0,
                                selected = selectedDate.day == day &&
                                    selectedDate.year == month.year && selectedDate.month == month.month,
                                enabled = interactive,
                                onClick = {
                                    onSelectDate(LocalDate(month.year, month.month.ordinal + 1, day))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        if (weekday < 6) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                if (week < weekCount - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun CalendarMonthDayCell(
    date: LocalDate,
    seconds: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = MaterialTheme.timnorSpacing.extraLarge * 2)
            .semantics { this.selected = selected },
        color = if (selected) colors.primaryContainer else colors.surface,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.timnorSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(date.day.toString(), style = MaterialTheme.typography.titleMedium)
            if (seconds > 0) {
                Text(
                    "${formatCalendarHours(seconds.toLong())}h",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
