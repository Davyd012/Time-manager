package no.timnor.app.shared.features.month_view.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
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
import no.timnor.app.shared.generated.resources.work_add
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewEmptyState
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewHeader
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewTimeline
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.new_work.WorkEntrySheet
import no.timnor.app.shared.features.month_view.presentation.components.monthViewWorksForDate
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimnorMonthViewScreen(
    state: TimnorHomeUiState,
    viewModel: TimnorHomeViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var showWorkEditor by rememberSaveable { mutableStateOf(false) }
    val month = state.calendarMonth
    val monthNumber = month.month.ordinal + 1
    val daysInMonth = remember(month.year, monthNumber) {
        monthGridLayout(month.year, monthNumber).daysInMonth
    }
    val monthWorks = remember(state.works, month) {
        state.works.filter { work ->
            val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
            date.year == month.year && date.month == month.month
        }
    }
    val secondsByDay = remember(monthWorks, month) {
        workSecondsByDay(
            entries = monthWorks,
            year = month.year,
            month = monthNumber,
            dateOf = { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date },
            secondsOf = StoredWork::time,
        )
    }
    val selectedWorks = remember(state.works, state.selectedCalendarDate) {
        monthViewWorksForDate(state.works, state.selectedCalendarDate)
    }
    val monthNames = listOf(
        stringResource(Res.string.calendar_month_january),
        stringResource(Res.string.calendar_month_february),
        stringResource(Res.string.calendar_month_march),
        stringResource(Res.string.calendar_month_april),
        stringResource(Res.string.calendar_month_may),
        stringResource(Res.string.calendar_month_june),
        stringResource(Res.string.calendar_month_july),
        stringResource(Res.string.calendar_month_august),
        stringResource(Res.string.calendar_month_september),
        stringResource(Res.string.calendar_month_october),
        stringResource(Res.string.calendar_month_november),
        stringResource(Res.string.calendar_month_december),
    )

    LaunchedEffect(state.editingWorkId) {
        if (state.editingWorkId != null) showWorkEditor = true
    }

    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.timnorSpacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
    ) {
        MonthViewHeader(
            monthTitle = "${monthNames[month.month.ordinal]} ${month.year}",
            daysInMonth = daysInMonth,
            secondsByDay = secondsByDay,
            selectedDate = state.selectedCalendarDate,
            onSelectDate = viewModel::selectCalendarDate,
            onBack = onBack,
        )
        if (selectedWorks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        viewModel.beginManualWork()
                        showWorkEditor = true
                    },
                    enabled = state.projects.isNotEmpty(),
                ) { Text(stringResource(Res.string.work_add)) }
            }
        }
        if (selectedWorks.isEmpty()) {
            MonthViewEmptyState(
                canAddWork = state.projects.isNotEmpty(),
                onAddWork = {
                    viewModel.beginManualWork()
                    showWorkEditor = true
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            MonthViewTimeline(
                works = selectedWorks,
                projects = state.projects,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onSelectWork = viewModel::editWork,
            )
        }
    }

    if (showWorkEditor) {
        WorkEntrySheet(
            state = state,
            onUpdateWorkDate = viewModel::updateWorkDate,
            onUpdateWorkStartTime = viewModel::updateWorkStartTime,
            onUpdateWorkHours = viewModel::updateWorkHours,
            onUpdateWorkMinutes = viewModel::updateWorkMinutes,
            onUpdateWorkDescription = viewModel::updateWorkDescription,
            onSelectWorkProject = viewModel::selectWorkProject,
            onSubmit = {
                viewModel.submitManualWork { saved ->
                    if (saved) showWorkEditor = false
                }
            },
            onDelete = { onResult ->
                val editingWork = state.works.firstOrNull { it.id == state.editingWorkId }
                if (editingWork == null) {
                    onResult(true)
                } else {
                    viewModel.deleteWork(editingWork, onResult)
                }
            },
            onSubmitPeriod = { dates ->
                viewModel.submitManualWorkPeriod(dates) { saved ->
                    if (saved) showWorkEditor = false
                }
            },
            onCancel = {
                showWorkEditor = false
                viewModel.cancelWorkEdit()
            },
            onDismiss = {
                showWorkEditor = false
                viewModel.cancelWorkEdit()
            },
        )
    }
}
