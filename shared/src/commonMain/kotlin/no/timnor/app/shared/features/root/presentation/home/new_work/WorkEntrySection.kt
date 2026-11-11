package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.domain.work.formatWorkDateInput
import no.timnor.app.shared.features.root.presentation.newProject.components.ProjectSelector
import no.timnor.app.shared.generated.resources.Res
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
import no.timnor.app.shared.generated.resources.home_weekday_friday
import no.timnor.app.shared.generated.resources.home_weekday_monday
import no.timnor.app.shared.generated.resources.home_weekday_saturday
import no.timnor.app.shared.generated.resources.home_weekday_sunday
import no.timnor.app.shared.generated.resources.home_weekday_thursday
import no.timnor.app.shared.generated.resources.home_weekday_tuesday
import no.timnor.app.shared.generated.resources.home_weekday_wednesday
import no.timnor.app.shared.generated.resources.work_date_header
import no.timnor.app.shared.generated.resources.work_date_label
import no.timnor.app.shared.generated.resources.work_period_label
import no.timnor.app.shared.generated.resources.work_period_range_label
import no.timnor.app.shared.generated.resources.work_period_select_range
import no.timnor.app.shared.generated.resources.work_period_single_date
import no.timnor.app.shared.generated.resources.work_project_hint
import no.timnor.app.shared.generated.resources.work_time_details
import no.timnor.app.shared.generated.resources.work_title
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkEntrySection(
    projects: List<StoredProject>,
    selectedProjectId: Int?,
    onProjectSelected: (Int) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    hours: String,
    onHoursChange: (String) -> Unit,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isEditing: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSubmitPeriod: (List<LocalDate>) -> Unit,
) {
    var periodRegistration by remember { mutableStateOf(false) }
    val periodRangeState = androidx.compose.material3.rememberDateRangePickerState()
    val periodDates = periodDatesFromMillis(
        periodRangeState.selectedStartDateMillis,
        periodRangeState.selectedEndDateMillis,
    )
    val dateHeader = PeriodHeader(periodRegistration, date, periodDates)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(
            stringResource(if (periodRegistration) Res.string.work_period_label else Res.string.work_date_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(dateHeader, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.work_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(Res.string.work_project_hint), style = MaterialTheme.typography.labelLarge)
        ProjectSelector(
            projects = projects,
            selectedProjectId = selectedProjectId,
            onProjectSelected = onProjectSelected,
        )
        Text(stringResource(Res.string.work_time_details), style = MaterialTheme.typography.labelLarge)
        WorkEntryManualFields(
            date = date,
            onDateChange = onDateChange,
            startTime = startTime,
            onStartTimeChange = onStartTimeChange,
            hours = hours,
            onHoursChange = onHoursChange,
            minutes = minutes,
            onMinutesChange = onMinutesChange,
            periodRegistration = periodRegistration,
            isEditing = isEditing,
        )
        WorkPeriodRegistration(
            enabled = periodRegistration,
            rangeState = periodRangeState,
        )
        WorkEntryDescriptionField(
            value = description,
            onValueChange = onDescriptionChange,
        )
        WorkPeriodRegistrationSwitch(
            enabled = periodRegistration,
            onEnabledChange = { periodRegistration = it },
        )
        WorkEntryActions(
            isEditing = isEditing,
            periodRegistration = periodRegistration,
            periodDates = periodDates,
            projectsExist = projects.isNotEmpty(),
            canSave = canSave,
            onSave = onSave,
            onDelete = onDelete,
            onSubmitPeriod = onSubmitPeriod,
        )
    }
}

@Composable
private fun PeriodHeader(
    periodRegistration: Boolean,
    date: String,
    periodDates: List<LocalDate>,
): String = when {
    !periodRegistration -> localizedWorkDateHeader(date)
    periodDates.isEmpty() -> stringResource(Res.string.work_period_select_range)
    periodDates.size == 1 -> stringResource(
        Res.string.work_period_single_date,
        localizedWorkDateHeader(periodDates.single().toString()),
    )
    else -> stringResource(
        Res.string.work_period_range_label,
        formatWorkDateInput(periodDates.first().toString()),
        formatWorkDateInput(periodDates.last().toString()),
    )
}

@Composable
private fun localizedWorkDateHeader(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val weekdayResource = when (date.dayOfWeek.ordinal) {
        0 -> Res.string.home_weekday_monday
        1 -> Res.string.home_weekday_tuesday
        2 -> Res.string.home_weekday_wednesday
        3 -> Res.string.home_weekday_thursday
        4 -> Res.string.home_weekday_friday
        5 -> Res.string.home_weekday_saturday
        else -> Res.string.home_weekday_sunday
    }
    val monthResource = when (date.month) {
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
    }
    return stringResource(
        Res.string.work_date_header,
        stringResource(weekdayResource),
        date.day.toString().padStart(2, '0'),
        stringResource(monthResource),
    )
}
