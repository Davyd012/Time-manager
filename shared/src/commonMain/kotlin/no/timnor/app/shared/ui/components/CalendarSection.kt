package no.timnor.app.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.domain.calendar.MonthGridLayout
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.calendar.totalWorkHours
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_day_no_entries
import no.timnor.app.shared.generated.resources.calendar_month_april
import no.timnor.app.shared.generated.resources.calendar_month_august
import no.timnor.app.shared.generated.resources.calendar_month_december
import no.timnor.app.shared.generated.resources.calendar_month_february
import no.timnor.app.shared.generated.resources.calendar_month_january
import no.timnor.app.shared.generated.resources.calendar_month_july
import no.timnor.app.shared.generated.resources.calendar_month_june
import no.timnor.app.shared.generated.resources.calendar_month_march
import no.timnor.app.shared.generated.resources.calendar_month_may
import no.timnor.app.shared.generated.resources.calendar_month_november
import no.timnor.app.shared.generated.resources.calendar_month_october
import no.timnor.app.shared.generated.resources.calendar_month_september
import no.timnor.app.shared.generated.resources.calendar_next_month
import no.timnor.app.shared.generated.resources.calendar_previous_month
import no.timnor.app.shared.generated.resources.project_filter_title
import no.timnor.app.shared.ui.theme.timnorSpacing
import no.timnor.app.shared.generated.resources.calendar_title
import no.timnor.app.shared.generated.resources.calendar_weekday_friday
import no.timnor.app.shared.generated.resources.calendar_weekday_monday
import no.timnor.app.shared.generated.resources.calendar_weekday_saturday
import no.timnor.app.shared.generated.resources.calendar_weekday_sunday
import no.timnor.app.shared.generated.resources.calendar_weekday_thursday
import no.timnor.app.shared.generated.resources.calendar_weekday_tuesday
import no.timnor.app.shared.generated.resources.calendar_weekday_wednesday
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarSection(
    month: LocalDate,
    selectedDate: LocalDate,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    works: List<StoredWork>,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (Int) -> Unit,
    onToggleProject: (Int) -> Unit,
    onEditWork: (StoredWork) -> Unit,
    onDeleteWork: (StoredWork) -> Unit,
) {
    val monthNumber = month.month.ordinal + 1
    val layout = remember(month.year, monthNumber) { monthGridLayout(month.year, monthNumber) }
    val byDay = remember(month, works) {
        workSecondsByDay(
            entries = works,
            year = month.year,
            month = monthNumber,
            dateOf = { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date },
            secondsOf = StoredWork::time,
        )
    }
    val selectedWorks = remember(works, projects, selectedProjectIds, selectedDate) {
        visibleWorks(works, projects, selectedProjectIds)
            .filter { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date == selectedDate }
            .sortedBy(StoredWork::dateEpochSeconds)
    }
    val monthTotal = totalWorkHours(
        visibleWorks(works, projects, selectedProjectIds).filter { work ->
            val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
            date.year == month.year && date.month == month.month
        },
        StoredWork::time,
    )
    val weekdays = listOf(
        stringResource(Res.string.calendar_weekday_monday),
        stringResource(Res.string.calendar_weekday_tuesday),
        stringResource(Res.string.calendar_weekday_wednesday),
        stringResource(Res.string.calendar_weekday_thursday),
        stringResource(Res.string.calendar_weekday_friday),
        stringResource(Res.string.calendar_weekday_saturday),
        stringResource(Res.string.calendar_weekday_sunday),
    )

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(stringResource(Res.string.calendar_title), style = MaterialTheme.typography.titleLarge)
        if (projects.isNotEmpty()) {
            Text(stringResource(Res.string.project_filter_title), style = MaterialTheme.typography.titleSmall)
            projects.forEach { project ->
                ProjectFilterRow(
                    projectId = project.id,
                    projectName = project.name,
                    monthlyHoursLabel = null,
                    checked = selectedProjectIds == null || project.id in selectedProjectIds,
                    onClick = { onToggleProject(project.id) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { onMonthChange(month.minus(1, DateTimeUnit.MONTH)) }) {
                Text(stringResource(Res.string.calendar_previous_month))
            }
            Text(
                text = "${monthName(month.month)} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = { onMonthChange(month.plus(1, DateTimeUnit.MONTH)) }) {
                Text(stringResource(Res.string.calendar_next_month))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { weekday ->
                Text(
                    text = weekday,
                    modifier = Modifier.weight(1f).padding(vertical = MaterialTheme.timnorSpacing.small),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        MonthGrid(
            month = month,
            layout = layout,
            secondsByDay = byDay,
            selectedDate = selectedDate,
            onSelectDate = onSelectDate,
        )
        Text("$monthTotal h", style = MaterialTheme.typography.titleMedium)
        Text(selectedDate.toString(), style = MaterialTheme.typography.titleSmall)
        if (selectedWorks.isEmpty()) {
            Text(stringResource(Res.string.calendar_day_no_entries))
        } else {
            selectedWorks.forEach { work ->
                RecentWorkRow(
                    work = work,
                    projectName = "",
                    onEdit = { onEditWork(work) },
                    onDelete = { onDeleteWork(work) },
                )
            }
        }
    }
}

private fun visibleWorks(
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
): List<StoredWork> {
    if (selectedProjectIds == null) return works
    val validProjectIds = projects.mapTo(mutableSetOf(), StoredProject::id)
    return works.filter { it.project in selectedProjectIds && it.project in validProjectIds }
}

@Composable
private fun MonthGrid(
    month: LocalDate,
    layout: MonthGridLayout,
    secondsByDay: Map<Int, Int>,
    selectedDate: LocalDate,
    onSelectDate: (Int) -> Unit,
) {
    val cells = layout.leadingEmptyDays + layout.daysInMonth + layout.trailingEmptyDays
    val weekCount = cells / 7
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        repeat(weekCount) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
            ) {
                repeat(7) { weekday ->
                    val cellIndex = week * 7 + weekday
                    val day = cellIndex - layout.leadingEmptyDays + 1
                    if (day in 1..layout.daysInMonth) {
                        CalendarDayCell(
                            date = LocalDate(month.year, month.month.ordinal + 1, day),
                            seconds = secondsByDay[day] ?: 0,
                            selected = selectedDate.day == day &&
                                selectedDate.year == month.year && selectedDate.month == month.month,
                            onClick = { onSelectDate(day) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    seconds: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface
    val hours = seconds / 3600
    val minutes = seconds / 60 % 60
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(date.day.toString(), style = MaterialTheme.typography.bodyMedium)
            Text("${hours}h ${minutes}m", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun monthName(month: Month): String = stringResource(
    when (month) {
        Month.JANUARY -> Res.string.calendar_month_january
        Month.FEBRUARY -> Res.string.calendar_month_february
        Month.MARCH -> Res.string.calendar_month_march
        Month.APRIL -> Res.string.calendar_month_april
        Month.MAY -> Res.string.calendar_month_may
        Month.JUNE -> Res.string.calendar_month_june
        Month.JULY -> Res.string.calendar_month_july
        Month.AUGUST -> Res.string.calendar_month_august
        Month.SEPTEMBER -> Res.string.calendar_month_september
        Month.OCTOBER -> Res.string.calendar_month_october
        Month.NOVEMBER -> Res.string.calendar_month_november
        Month.DECEMBER -> Res.string.calendar_month_december
    },
)
