package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.calendar.presentation.components.ProjectFilterSheet
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_closed_state
import no.timnor.app.shared.generated.resources.calendar_open_state
import no.timnor.app.shared.generated.resources.calendar_open_month_view
import no.timnor.app.shared.generated.resources.calendar_weekday_friday
import no.timnor.app.shared.generated.resources.calendar_weekday_monday
import no.timnor.app.shared.generated.resources.calendar_weekday_saturday
import no.timnor.app.shared.generated.resources.calendar_weekday_sunday
import no.timnor.app.shared.generated.resources.calendar_weekday_thursday
import no.timnor.app.shared.generated.resources.calendar_weekday_tuesday
import no.timnor.app.shared.generated.resources.calendar_weekday_wednesday
import no.timnor.app.shared.generated.resources.close_calendar_action
import no.timnor.app.shared.generated.resources.open_calendar_action
import no.timnor.app.shared.generated.resources.project_filter_active
import no.timnor.app.shared.generated.resources.project_filter_title
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay

@Composable
internal fun CalendarSection(
    month: LocalDate,
    selectedDate: LocalDate,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    works: List<StoredWork>,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onApplyProjectFilter: (Set<Int>?) -> Unit,
    onMonthView: (LocalDate) -> Unit,
    onSetExpanded: (Boolean) -> Unit,
    onExpansionProgressChange: (Float) -> Unit,
    expansionDragDistancePx: Float,
    expansionVelocityThresholdPx: Float,
    onShareMonth: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    expansionProgress: Float = if (expanded) 1f else 0f,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val systemAnimationsEnabled = rememberSystemAnimationsEnabled()
    val expandedContentEnabled = expansionProgress >= 0.5f
    val toggleExpanded = onToggleExpanded
    val calendarStateDescription = toggleExpanded?.let {
        stringResource(if (expanded) Res.string.calendar_open_state else Res.string.calendar_closed_state)
    }
    val toggleActionLabel = toggleExpanded?.let {
        stringResource(if (expanded) Res.string.close_calendar_action else Res.string.open_calendar_action)
    }
    val closeCalendarLabel = stringResource(Res.string.close_calendar_action)
    val monthViewLabel = stringResource(Res.string.calendar_open_month_view)
    val filterLabel = if (selectedProjectIds == null) {
        stringResource(Res.string.project_filter_title)
    } else {
        stringResource(Res.string.project_filter_active, selectedProjectIds.size, projects.size)
    }
    var showFilterSheet by remember { mutableStateOf(false) }
    var monthTransitioning by remember { mutableStateOf(false) }
    var selectedGridDateText by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGridDate = selectedGridDateText?.let(LocalDate::parse)
    val weekdays = listOf(
        stringResource(Res.string.calendar_weekday_monday),
        stringResource(Res.string.calendar_weekday_tuesday),
        stringResource(Res.string.calendar_weekday_wednesday),
        stringResource(Res.string.calendar_weekday_thursday),
        stringResource(Res.string.calendar_weekday_friday),
        stringResource(Res.string.calendar_weekday_saturday),
        stringResource(Res.string.calendar_weekday_sunday),
    )
    val latestExpansionProgress = rememberUpdatedState(expansionProgress)
    val latestOnSetExpanded = rememberUpdatedState(onSetExpanded)
    val latestOnExpansionProgressChange = rememberUpdatedState(onExpansionProgressChange)
    val latestExpansionDragDistance = rememberUpdatedState(expansionDragDistancePx)
    val latestVelocityThreshold = rememberUpdatedState(expansionVelocityThresholdPx)
    var dragProgress by remember { mutableFloatStateOf(expansionProgress) }
    val expansionDragState = rememberDraggableState { delta ->
        dragProgress = (dragProgress + delta / latestExpansionDragDistance.value).coerceIn(0f, 1f)
        latestOnExpansionProgressChange.value(dragProgress)
    }
    val weekStart = remember(selectedDate) {
        selectedDate.minus(selectedDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }
    val weekDays = remember(weekStart) { List(7) { weekStart.plus(it, DateTimeUnit.DAY) } }
    val weekSeconds = remember(works, weekDays) {
        weekDays.associateWith { day ->
            works.asSequence()
                .filter { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date == day }
                .sumOf(StoredWork::time)
        }
    }

    LaunchedEffect(month) {
        if (monthTransitioning) {
            delay(if (systemAnimationsEnabled) 320 else 0)
            monthTransitioning = false
        }
    }

    fun requestMonthChange(targetMonth: LocalDate) {
        if (expandedContentEnabled && !monthTransitioning && targetMonth != month) {
            monthTransitioning = true
            onMonthChange(targetMonth)
        }
    }

    Column(
        modifier = modifier
            .then(
                if (toggleExpanded == null) Modifier else Modifier.semantics {
                    stateDescription = requireNotNull(calendarStateDescription)
                    customActions = listOf(
                        CustomAccessibilityAction(requireNotNull(toggleActionLabel)) {
                            toggleExpanded()
                            true
                        },
                    )
                },
            )
            .animateContentSize(
                animationSpec = if (systemAnimationsEnabled) {
                    spring(dampingRatio = 0.82f, stiffness = 420f)
                } else {
                    snap()
                },
            )
            .draggable(
                state = expansionDragState,
                orientation = Orientation.Vertical,
                onDragStarted = { dragProgress = latestExpansionProgress.value },
                onDragStopped = { velocity ->
                    latestOnSetExpanded.value(
                        calendarExpansionTarget(
                            progress = dragProgress,
                            velocityPxPerSecond = velocity,
                            velocityThresholdPxPerSecond = latestVelocityThreshold.value,
                        ),
                    )
                },
            )
            .then(if (expansionProgress > 0f) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        if (expansionProgress > 0.01f) {
            CalendarExpandedToolbar(
                month = month,
                expandedContentEnabled = expandedContentEnabled,
                expansionProgress = expansionProgress,
                filterActive = selectedProjectIds != null,
                closeLabel = closeCalendarLabel,
                monthViewLabel = monthViewLabel,
                filterLabel = filterLabel,
                onToggleExpanded = onToggleExpanded,
                onMonthView = onMonthView,
                onShowFilter = { showFilterSheet = true },
                dragState = expansionDragState,
                onDragStarted = { dragProgress = latestExpansionProgress.value },
                onDragStopped = { velocity ->
                    latestOnSetExpanded.value(
                        calendarExpansionTarget(
                            progress = dragProgress,
                            velocityPxPerSecond = velocity,
                            velocityThresholdPxPerSecond = latestVelocityThreshold.value,
                        ),
                    )
                },
            )
        }
        CalendarProgressContent(
            month = month,
            selectedDate = selectedDate,
            selectedGridDate = selectedGridDate,
            works = works,
            projects = projects,
            selectedProjectIds = selectedProjectIds,
            weekdays = weekdays,
            weekDays = weekDays,
            weekSeconds = weekSeconds,
            expansionProgress = expansionProgress,
            expanded = expanded,
                expandedContentEnabled = expansionProgress >= 0.999f && !monthTransitioning,
            animationsEnabled = systemAnimationsEnabled,
            onMonthChange = ::requestMonthChange,
            onSelectDate = onSelectDate,
            onSelectGridDate = { selectedGridDateText = it.toString() },
            onMonthView = onMonthView,
            onToggleExpanded = onToggleExpanded,
            onShareMonth = onShareMonth,
        )
    }

    if (showFilterSheet) {
        ProjectFilterSheet(
            projects = projects,
            works = works,
            month = month,
            selectedProjectIds = selectedProjectIds,
            onApply = onApplyProjectFilter,
            onDismiss = { showFilterSheet = false },
        )
    }
}

