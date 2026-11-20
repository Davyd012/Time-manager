package no.timnor.app.shared.features.root.presentation.home

import no.timnor.app.shared.domain.calendar.calendarVisibleWorks
import androidx.compose.runtime.remember
import no.timnor.app.shared.generated.resources.invalid_hourly_rate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import no.timnor.app.shared.features.calendar.presentation.components.TimnorMonthExportDialog
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.components.CalendarSection
import no.timnor.app.shared.features.root.presentation.home.components.calendarExpansionTarget
import no.timnor.app.shared.features.root.presentation.home.calendarExpansionTravel
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
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
    onCalendarBackEnabledChange: (Boolean) -> Unit = {},
    /** Deterministic progress control for screenshot tests; normal app callers leave this unset. */
    testExpansionProgress: Float? = null,
    modifier: Modifier = Modifier,
) {
    val projects = state.projects
    val works = state.works
    val visibleWorks = remember(works, projects, state.selectedCalendarProjectIds) {
        calendarVisibleWorks(works, projects, state.selectedCalendarProjectIds)
    }
    var showWorkEditor by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var savedCalendarExpanded by rememberSaveable { mutableStateOf(false) }
    var calendarScrollMode by rememberSaveable { mutableStateOf(false) }
    val expansionAnimation = androidx.compose.runtime.remember { Animatable(if (savedCalendarExpanded) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberSystemAnimationsEnabled()
    var isDraggingCalendar by androidx.compose.runtime.remember { mutableStateOf(false) }
    var expansionJob by androidx.compose.runtime.remember { mutableStateOf<Job?>(null) }
    val calendarProgress = testExpansionProgress?.coerceIn(0f, 1f) ?: expansionAnimation.value
    val calendarExpanded = calendarProgress >= 0.5f
    val calendarBackEnabled = calendarProgress > 0.001f
    val calendarScrollState = rememberScrollState()

    suspend fun updateCalendarExpansionProgress(progress: Float) {
        expansionAnimation.snapTo(progress.coerceIn(0f, 1f))
    }

    fun startCalendarDrag() {
        expansionJob?.cancel()
        isDraggingCalendar = true
    }

    fun settleCalendarExpansion(expanded: Boolean) {
        savedCalendarExpanded = expanded
        isDraggingCalendar = false
        expansionJob?.cancel()
        expansionJob = scope.launch {
            expansionAnimation.animateTo(
                if (expanded) 1f else 0f,
                if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else snap(),
            )
        }
    }

    LaunchedEffect(state.editingWorkId) {
        if (state.editingWorkId != null) showWorkEditor = true
    }
    LaunchedEffect(homeReselectionCount) {
        if (homeReselectionCount > 0) {
            expansionJob?.cancel()
            savedCalendarExpanded = false
            isDraggingCalendar = false
            expansionAnimation.animateTo(0f, if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else snap())
        }
    }
    LaunchedEffect(calendarExpanded) { onCalendarExpansionChange(calendarExpanded) }
    LaunchedEffect(calendarBackEnabled) { onCalendarBackEnabledChange(calendarBackEnabled) }
    LaunchedEffect(calendarProgress >= 0.999f) {
        if (calendarProgress < 0.999f && calendarScrollState.value > 0) {
            calendarScrollState.scrollTo(0)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().clipToBounds()) {
            var collapsedHomeHeightPx by androidx.compose.runtime.remember(maxHeight) {
                androidx.compose.runtime.mutableIntStateOf(0)
            }
            val velocityThreshold = with(LocalDensity.current) { 1000.dp.toPx() }
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val monthGrid = monthGridLayout(state.calendarMonth.year, state.calendarMonth.month.ordinal + 1)
            val monthRows = (monthGrid.leadingEmptyDays + monthGrid.daysInMonth + monthGrid.trailingEmptyDays) / 7
            val spacing = MaterialTheme.timnorSpacing
            val dragDistance = with(LocalDensity.current) {
                calendarExpansionTravel(monthRows, fontScale, spacing).toPx()
            }
            val homeOffsetProgress = if (calendarProgress <= 0.75f) {
                calendarProgress.coerceIn(0f, 1f) * 0.8f
            } else {
                0.6f + ((calendarProgress - 0.75f) / 0.25f).coerceIn(0f, 1f) * 0.45f
            }
            Column(
                modifier = Modifier.fillMaxSize()
                    .testTag("home-calendar-gesture-surface")
                    .calendarExpansionGesture(
                        enabled = !calendarScrollMode,
                        dragDistancePx = dragDistance,
                        currentProgress = { expansionAnimation.value },
                        onDragStart = ::startCalendarDrag,
                        onProgressChange = ::updateCalendarExpansionProgress,
                        onDragStop = { velocity ->
                            settleCalendarExpansion(
                                calendarExpansionTarget(expansionAnimation.value, velocity, velocityThreshold),
                            )
                        },
                    ),
            ) {
            HomeHeaderBar(
                selectedDate = state.selectedCalendarDate,
                expansionProgress = calendarProgress,
                visibilityProgress = ((calendarProgress - 0.4f) / 0.15f).coerceIn(0f, 1f),
                canAddWork = projects.isNotEmpty(),
                onToggleCalendar = { settleCalendarExpansion(calendarProgress < 0.5f) },
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
                            TimnorHomeError.INVALID_HOURLY_RATE -> Res.string.invalid_hourly_rate
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
                linkedPlannorProjectNames = state.linkedPlannorProjectNames,
                sharedProjectIndicators = state.sharedWorkIndicatorsByProject,
                selectedProjectIds = state.selectedCalendarProjectIds,
                works = works,
                onMonthChange = viewModel::updateCalendarMonth,
                onSelectDate = viewModel::selectCalendarDate,
                onApplyProjectFilter = viewModel::setCalendarProjectSelection,
                onMonthView = onMonthView,
                onShareMonth = onShareMonth,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = MaterialTheme.timnorSpacing.medium)
                    .zIndex(1f),
                expanded = calendarExpanded,
                expansionProgress = calendarProgress,
                scrollMode = calendarScrollMode,
                scrollState = calendarScrollState,
                onToggleScrollMode = { calendarScrollMode = !calendarScrollMode },
                contentInteractive = calendarProgress >= 0.999f && !isDraggingCalendar,
                onToggleExpanded = { settleCalendarExpansion(calendarProgress < 0.5f) },
            )
            HomeDailyWorkList(
                selectedDate = state.selectedCalendarDate,
                works = visibleWorks,
                projects = projects,
                linkedPlannorProjectNames = state.linkedPlannorProjectNames,
                sharedWorkIndicators = state.sharedWorkIndicators,
                expansionProgress = calendarProgress,
                isDraggingCalendar = isDraggingCalendar,
                modifier = Modifier.fillMaxWidth()
                    .then(
                        if (collapsedHomeHeightPx > 0) {
                            Modifier.requiredHeight(with(LocalDensity.current) { collapsedHomeHeightPx.toDp() })
                        } else {
                            Modifier.weight(1f)
                        },
                    )
                    .offset {
                        IntOffset(x = 0, y = (dragDistance * homeOffsetProgress).roundToInt())
                    }
                    .onSizeChanged { size ->
                        if (calendarProgress <= 0.001f && size.height > 0 && collapsedHomeHeightPx != size.height) {
                            collapsedHomeHeightPx = size.height
                        }
                    },
                onEditWork = viewModel::editWork,
                onAddWork = {
                    viewModel.beginManualWork()
                    showWorkEditor = true
                },
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
            onBeginProject = viewModel::cancelProjectEdit,
            onProjectNameChange = viewModel::updateProjectName,
            onProjectDescriptionChange = viewModel::updateProjectDescription,
            onProjectHourlyRateChange = viewModel::updateProjectHourlyRate,
            onSaveProject = viewModel::saveProjectForWork,
            onCancelProject = viewModel::cancelProjectEdit,
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
            showDragHandle = true,
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
