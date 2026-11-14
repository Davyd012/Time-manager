package no.timnor.app.shared.features.month_view.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewEmptyState
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewHeader
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewTimeline
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.new_work.WorkEntrySheet
import no.timnor.app.shared.features.month_view.presentation.components.monthViewWorksForDate
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
fun TimnorMonthViewScreen(
    state: TimnorHomeUiState,
    viewModel: TimnorHomeViewModel,
    modifier: Modifier = Modifier,
    scrollToSelectedDateKey: Int = 0,
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
    LaunchedEffect(state.editingWorkId) {
        if (state.editingWorkId != null) showWorkEditor = true
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            MonthViewHeader(
            daysInMonth = daysInMonth,
            secondsByDay = secondsByDay,
            selectedDate = state.selectedCalendarDate,
                scrollToSelectedDateKey = scrollToSelectedDateKey,
            onSelectDate = viewModel::selectCalendarDate,
            onBack = onBack,
        )
            if (selectedWorks.isEmpty()) {
            MonthViewEmptyState(
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
                linkedPlannorProjectNames = state.linkedPlannorProjectNames,
                sharedWorkIndicators = state.sharedWorkIndicators,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onSelectWork = viewModel::editWork,
            )
            }
        }
    }

    if (showWorkEditor) {
        WorkEntrySheet(
            state = state,
            linkedPlannorProjectNames = state.linkedPlannorProjectNames,
            onUpdateWorkDate = viewModel::updateWorkDate,
            onUpdateWorkStartTime = viewModel::updateWorkStartTime,
            onUpdateWorkHours = viewModel::updateWorkHours,
            onUpdateWorkMinutes = viewModel::updateWorkMinutes,
            onUpdateWorkDescription = viewModel::updateWorkDescription,
            onSelectWorkProject = viewModel::selectWorkProject,
            onSelectPlannorTask = viewModel::selectPlannorTask,
            onSubmit = {
                viewModel.submitManualWork { saved ->
                    if (saved) showWorkEditor = false
                }
            },
            onDelete = { confirmed, onResult ->
                val editingWork = state.works.firstOrNull { it.id == state.editingWorkId }
                if (editingWork == null) {
                    onResult(true)
                } else {
                    viewModel.deleteWork(editingWork, onResult, sharedDeletionConfirmed = confirmed)
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
            showDragHandle = false,
        )
    }
}
