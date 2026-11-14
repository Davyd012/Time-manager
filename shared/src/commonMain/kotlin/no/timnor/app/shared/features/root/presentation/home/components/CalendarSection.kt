package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.core.repository.SharedWorkSyncIndicator
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
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarSection(
    month: LocalDate,
    selectedDate: LocalDate,
    projects: List<StoredProject>,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedProjectIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    selectedProjectIds: Set<Int>?,
    works: List<StoredWork>,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onApplyProjectFilter: (Set<Int>?) -> Unit,
    onMonthView: (LocalDate) -> Unit,
    onShareMonth: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    expansionProgress: Float = if (expanded) 1f else 0f,
    scrollMode: Boolean = false,
    onToggleScrollMode: () -> Unit = {},
    contentInteractive: Boolean = expansionProgress >= 0.999f,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val expandedContentEnabled = contentInteractive
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
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val toolbarSlotHeight = (MaterialTheme.timnorSpacing.extraLarge * 2 + MaterialTheme.timnorSpacing.extraSmall) * fontScale
    val toolbarVisibility = ((expansionProgress - 0.45f) / 0.30f).coerceIn(0f, 1f)
    var showFilterSheet by remember { mutableStateOf(false) }
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
    val calendarScrollState = rememberScrollState()
    fun requestMonthChange(targetMonth: LocalDate) {
        if (expandedContentEnabled && targetMonth != month) {
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
            .then(
                if (expansionProgress > 0f) Modifier.verticalScroll(calendarScrollState) else Modifier,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small * expansionProgress.coerceIn(0f, 1f)),
    ) {
        CalendarExpandedToolbar(
            modifier = Modifier.offset(y = -toolbarSlotHeight * (1f - expansionProgress.coerceIn(0f, 1f))),
            month = month,
            expandedContentEnabled = expandedContentEnabled,
            expansionProgress = expansionProgress,
            visibilityProgress = toolbarVisibility,
            filterActive = selectedProjectIds != null,
            closeLabel = closeCalendarLabel,
            monthViewLabel = monthViewLabel,
            filterLabel = filterLabel,
            scrollMode = scrollMode,
            onToggleScrollMode = onToggleScrollMode,
            onToggleExpanded = onToggleExpanded,
            onMonthView = onMonthView,
            onShowFilter = { showFilterSheet = true },
        )
        CalendarProgressContent(
            month = month,
            selectedDate = selectedDate,
            selectedGridDate = selectedGridDate,
            works = works,
            projects = projects,
            selectedProjectIds = selectedProjectIds,
            weekdays = weekdays,
            expansionProgress = expansionProgress,
            expanded = expanded,
            expandedContentEnabled = contentInteractive,
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
            linkedPlannorProjectNames = linkedPlannorProjectNames,
            sharedProjectIndicators = sharedProjectIndicators,
            selectedProjectIds = selectedProjectIds,
            onApply = onApplyProjectFilter,
            onDismiss = { showFilterSheet = false },
        )
    }
}

