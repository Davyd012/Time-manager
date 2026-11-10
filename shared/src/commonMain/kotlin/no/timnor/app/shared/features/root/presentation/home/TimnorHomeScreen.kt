package no.timnor.app.shared.features.root.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import no.timnor.app.shared.features.calendar.presentation.components.TimnorMonthExportDialog
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.components.CalendarSection
import no.timnor.app.shared.features.root.presentation.home.components.HomeDailyWorkList
import no.timnor.app.shared.features.root.presentation.home.components.HomeHeaderBar
import no.timnor.app.shared.features.root.presentation.home.new_work.WorkEntrySheet
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsSection
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.invalid_project
import no.timnor.app.shared.generated.resources.invalid_work
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.generated.resources.stopwatch_project_locked
import no.timnor.app.shared.ui.theme.timnorSpacing
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimnorHomeScreen(
    state: TimnorHomeUiState,
    viewModel: TimnorHomeViewModel,
    homeReselectionCount: Int,
    onMonthView: (LocalDate) -> Unit,
    onSettings: () -> Unit = {},
    onExportSpreadsheet: ((Int, Int, Set<Int>?) -> Unit)? = null,
    onShareMonth: ((String, String) -> Unit)? = null,
    onCalendarExpansionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val projects = state.projects
    val works = state.works
    var showWorkEditor by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    val expansionAnimation = androidx.compose.runtime.remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberSystemAnimationsEnabled()
    var dragProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var isDraggingCalendar by rememberSaveable { mutableStateOf(false) }
    val calendarProgress = if (isDraggingCalendar) dragProgress else expansionAnimation.value
    val calendarExpanded = calendarProgress >= 0.5f

    fun updateCalendarExpansionProgress(progress: Float) {
        isDraggingCalendar = true
        dragProgress = progress
    }

    fun settleCalendarExpansion(expanded: Boolean) {
        val start = if (isDraggingCalendar) dragProgress else expansionAnimation.value
        isDraggingCalendar = false
        scope.launch {
            expansionAnimation.snapTo(start)
            expansionAnimation.animateTo(
                if (expanded) 1f else 0f,
                if (animationsEnabled) spring(stiffness = Spring.StiffnessMedium) else snap(),
            )
        }
    }

    LaunchedEffect(state.editingWorkId) {
        if (state.editingWorkId != null) showWorkEditor = true
    }
    LaunchedEffect(homeReselectionCount) {
        if (homeReselectionCount > 0) {
            isDraggingCalendar = false
            expansionAnimation.animateTo(0f, if (animationsEnabled) spring(stiffness = Spring.StiffnessMedium) else snap())
        }
    }
    LaunchedEffect(calendarExpanded) { onCalendarExpansionChange(calendarExpanded) }

    Column(
        modifier = modifier.fillMaxSize().padding(
            horizontal = MaterialTheme.timnorSpacing.medium,
            vertical = MaterialTheme.timnorSpacing.small,
        ),
    ) {
        HomeHeaderBar(
            selectedDate = state.selectedCalendarDate,
            expansionProgress = calendarProgress,
            canAddWork = projects.isNotEmpty(),
            onToggleCalendar = { settleCalendarExpansion(calendarProgress < 0.5f) },
            onSetExpanded = ::settleCalendarExpansion,
            onExpansionProgressChange = ::updateCalendarExpansionProgress,
            onSettings = onSettings,
            onExport = onExportSpreadsheet?.let { { showExportDialog = true } },
            onAddWork = {
                viewModel.beginManualWork()
                showWorkEditor = true
            },
        )
        state.error?.let { error ->
            Text(
                text = stringResource(
                    when (error) {
                        TimnorHomeError.INVALID_PROJECT -> Res.string.invalid_project
                        TimnorHomeError.INVALID_WORK -> Res.string.invalid_work
                        TimnorHomeError.STORAGE -> Res.string.storage_error
                        TimnorHomeError.ACTIVE_STOPWATCH_PROJECT -> Res.string.stopwatch_project_locked
                    },
                ),
                modifier = Modifier.padding(horizontal = MaterialTheme.timnorSpacing.medium),
                color = MaterialTheme.colorScheme.error,
            )
        }
        CalendarSection(
            month = state.calendarMonth,
            selectedDate = state.selectedCalendarDate,
            projects = projects,
            selectedProjectIds = state.selectedCalendarProjectIds,
            works = works,
            onMonthChange = viewModel::updateCalendarMonth,
            onSelectDate = viewModel::selectCalendarDate,
            onApplyProjectFilter = viewModel::setCalendarProjectSelection,
            onMonthView = onMonthView,
            onShareMonth = onShareMonth,
            onSetExpanded = ::settleCalendarExpansion,
            onExpansionProgressChange = ::updateCalendarExpansionProgress,
            modifier = if (calendarProgress > 0f) {
                Modifier.fillMaxWidth().weight(0.25f + calendarProgress * 0.75f)
            } else Modifier.fillMaxWidth(),
            expanded = calendarExpanded,
            expansionProgress = calendarProgress,
            onToggleExpanded = { settleCalendarExpansion(calendarProgress < 0.5f) },
        )
        if (calendarProgress < 1f) {
            HomeDailyWorkList(
                selectedDate = state.selectedCalendarDate,
                works = works,
                projects = projects,
                canAddWork = projects.isNotEmpty(),
                expansionProgress = calendarProgress,
                modifier = Modifier.fillMaxWidth().weight((1f - calendarProgress).coerceAtLeast(0.01f)),
                onEditWork = viewModel::editWork,
                onAddWork = {
                    viewModel.beginManualWork()
                    showWorkEditor = true
                },
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

    if (showExportDialog && onExportSpreadsheet != null) {
        TimnorMonthExportDialog(
            projects = projects,
            onDismiss = { showExportDialog = false },
            onExport = { year, month, projectIds ->
                showExportDialog = false
                onExportSpreadsheet(year, month, projectIds)
            },
        )
    }
}
