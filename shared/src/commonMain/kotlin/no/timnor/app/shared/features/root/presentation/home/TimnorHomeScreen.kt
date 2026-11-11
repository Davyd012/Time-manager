package no.timnor.app.shared.features.root.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import no.timnor.app.shared.features.calendar.presentation.components.TimnorMonthExportDialog
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.components.CalendarSection
import no.timnor.app.shared.features.root.presentation.home.components.HomeDailyWorkList
import no.timnor.app.shared.features.root.presentation.home.components.HomeHeaderBar
import no.timnor.app.shared.features.root.presentation.home.components.emptyListExpansionTarget
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
    modifier: Modifier = Modifier,
) {
    val projects = state.projects
    val works = state.works
    var showWorkEditor by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var savedCalendarExpanded by rememberSaveable { mutableStateOf(false) }
    val expansionAnimation = androidx.compose.runtime.remember { Animatable(if (savedCalendarExpanded) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberSystemAnimationsEnabled()
    var dragProgress by androidx.compose.runtime.remember { mutableFloatStateOf(expansionAnimation.value) }
    var isDraggingCalendar by androidx.compose.runtime.remember { mutableStateOf(false) }
    var expansionJob by androidx.compose.runtime.remember { mutableStateOf<Job?>(null) }
    val calendarProgress = if (isDraggingCalendar) dragProgress else expansionAnimation.value
    val calendarExpanded = calendarProgress >= 0.5f
    val calendarBackEnabled = calendarProgress > 0.001f

    fun updateCalendarExpansionProgress(progress: Float) {
        if (!isDraggingCalendar) expansionJob?.cancel()
        isDraggingCalendar = true
        dragProgress = progress.coerceIn(0f, 1f)
    }

    fun settleCalendarExpansion(expanded: Boolean) {
        val start = if (isDraggingCalendar) dragProgress else expansionAnimation.value
        savedCalendarExpanded = expanded
        expansionJob?.cancel()
        expansionJob = scope.launch {
            expansionAnimation.snapTo(start)
            isDraggingCalendar = false
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
            val collapseStartProgress = homeCalendarCollapseStartProgress(
                isDragging = isDraggingCalendar,
                dragProgress = dragProgress,
                animationProgress = expansionAnimation.value,
            )
            savedCalendarExpanded = false
            expansionAnimation.snapTo(collapseStartProgress)
            isDraggingCalendar = false
            expansionAnimation.animateTo(0f, if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else snap())
        }
    }
    LaunchedEffect(calendarExpanded) { onCalendarExpansionChange(calendarExpanded) }
    LaunchedEffect(calendarBackEnabled) { onCalendarBackEnabledChange(calendarBackEnabled) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().clipToBounds()) {
            val dragDistance = constraints.maxHeight.coerceAtLeast(1).toFloat()
            val velocityThreshold = with(LocalDensity.current) { 1000.dp.toPx() }
            val positionalThreshold = with(LocalDensity.current) {
                (MaterialTheme.timnorSpacing.extraLarge + MaterialTheme.timnorSpacing.medium + MaterialTheme.timnorSpacing.small).toPx()
            }
            val emptyListDragState = rememberDraggableState { delta ->
                updateCalendarExpansionProgress((dragProgress + delta / dragDistance).coerceIn(0f, 1f))
            }
            fun startEmptyListDrag() {
                expansionJob?.cancel()
                dragProgress = calendarProgress
                isDraggingCalendar = true
            }
            fun stopEmptyListDrag(velocity: Float) {
                val expanded = emptyListExpansionTarget(
                    progress = dragProgress,
                    startExpanded = savedCalendarExpanded,
                    velocity = velocity,
                    velocityThreshold = velocityThreshold,
                    distance = dragDistance,
                    positionalThreshold = positionalThreshold,
                )
                settleCalendarExpansion(expanded)
            }
            Column(modifier = Modifier.fillMaxSize().then(
                if (calendarProgress >= 0.999f) Modifier.clearAndSetSemantics {} else Modifier,
            )) {
            HomeHeaderBar(
                selectedDate = state.selectedCalendarDate,
                expansionProgress = 0f,
                gestureProgress = calendarProgress,
                canAddWork = projects.isNotEmpty(),
                onToggleCalendar = { settleCalendarExpansion(calendarProgress < 0.5f) },
                onSetExpanded = ::settleCalendarExpansion,
                onExpansionProgressChange = ::updateCalendarExpansionProgress,
                expansionDragDistancePx = dragDistance,
                expansionVelocityThresholdPx = velocityThreshold,
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
                expansionDragDistancePx = dragDistance,
                expansionVelocityThresholdPx = velocityThreshold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.timnorSpacing.medium),
                expanded = false,
                expansionProgress = 0f,
                gestureProgress = calendarProgress,
                onToggleExpanded = { settleCalendarExpansion(calendarProgress < 0.5f) },
            )
            HomeDailyWorkList(
                selectedDate = state.selectedCalendarDate,
                works = works,
                projects = projects,
                expansionProgress = calendarProgress,
                expansionDragState = emptyListDragState,
                onExpansionDragStarted = ::startEmptyListDrag,
                onExpansionDragStopped = ::stopEmptyListDrag,
                isDraggingCalendar = isDraggingCalendar,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onEditWork = viewModel::editWork,
                onAddWork = {
                    viewModel.beginManualWork()
                    showWorkEditor = true
                },
            )
            }
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val panel = measurable.measure(constraints.copy(minHeight = constraints.maxHeight))
                        val visibleHeight = calendarPanelVisibleHeight(calendarProgress, panel.height)
                        layout(panel.width, visibleHeight) { panel.place(0, 0) }
                    }
                    .then(if (calendarProgress <= 0.001f) Modifier.clearAndSetSemantics {} else Modifier),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
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
                    expansionDragDistancePx = dragDistance,
                    expansionVelocityThresholdPx = velocityThreshold,
                    modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.timnorSpacing.medium),
                    expanded = true,
                    expansionProgress = 1f,
                    gestureProgress = calendarProgress,
                    contentInteractive = calendarProgress >= 0.999f && !isDraggingCalendar,
                    onToggleExpanded = { settleCalendarExpansion(false) },
                )
            }
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

internal fun homeCalendarCollapseStartProgress(
    isDragging: Boolean,
    dragProgress: Float,
    animationProgress: Float,
): Float = (if (isDragging) dragProgress else animationProgress).coerceIn(0f, 1f)

internal fun calendarPanelVisibleHeight(progress: Float, height: Int): Int =
    (progress.coerceIn(0f, 1f) * height.coerceAtLeast(0)).roundToInt()
