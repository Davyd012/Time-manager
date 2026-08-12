package org.examples.time_manager.features.root.presentation.home.components

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import java.time.LocalDate
import org.examples.time_manager.R
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.features.calendar.presentation.components.CalendarSelectorRow
import org.examples.time_manager.features.calendar.presentation.components.calendarMonthSwipeGesture
import org.examples.time_manager.features.calendar.presentation.components.DayWork
import org.examples.time_manager.features.calendar.presentation.components.MonthGrid
import org.examples.time_manager.features.calendar.presentation.components.MonthViewColors
import org.examples.time_manager.features.calendar.presentation.components.ProjectFilterSheet
import org.examples.time_manager.features.root.data.HomeState
import org.examples.time_manager.features.root.data.HomeIntent
import org.examples.time_manager.features.root.data.HomeIntent.ChangeCalendarMonth
import org.examples.time_manager.features.root.data.HomeIntent.CreateExcelDocument
import org.examples.time_manager.features.root.data.HomeIntent.SelectDay
import org.examples.time_manager.features.root.data.HomeIntent.SetCalendarProjects
import org.examples.time_manager.features.root.presentation.home.CalendarExpansion
import org.examples.time_manager.features.root.presentation.home.MonthPickerDialog
import org.examples.time_manager.features.utils.formatHoursFromSeconds
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.addIcon
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.dateRangeIcon
import org.examples.time_manager.ui.theme.exportIcon
import org.examples.time_manager.ui.theme.filterIcon
import org.examples.time_manager.ui.theme.spacing
import java.time.format.TextStyle

@Composable
fun HeaderWidget(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    navigator: Navigator,
    expansionState: AnchoredDraggableState<CalendarExpansion>,
    availableHeightPx: Int,
    selectedCalendarDate: LocalDate?,
    onCalendarDateSelected: (LocalDate) -> Unit,
    onAddWork: () -> Unit,
    onToggleCalendar: () -> Unit,
    onCloseCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var showProjectFilter by remember { mutableStateOf(false) }
    var monthTransitioning by remember { mutableStateOf(false) }
    var requestedMonth by remember { mutableStateOf<java.time.YearMonth?>(null) }
    val months = stringArrayResource(R.array.months_array).toList()
    val context = LocalContext.current

    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val month = result.data?.getStringExtra("stringKey")?.toIntOrNull()
                result.data?.data?.let { uri ->
                    onIntent(CreateExcelDocument(uri.toString(), month))
                }
            }
        }

    val openSaveFilePicker = remember(months, saveFileLauncher) {
        { month: Int ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "${months.elementAt(month)}.xlsx")
                putExtra("stringKey", month)
            }
            saveFileLauncher.launch(intent)
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            onDismiss = { month ->
                if (month >= 0) openSaveFilePicker(month)
                showMonthPicker = false
            },
            onIntent = onIntent,
            projectValues = state.projects,
        )
    }

    if (showProjectFilter) {
        ProjectFilterSheet(
            projects = state.projects,
            selectedProjects = state.calendarSelectedProjects,
            projectHours = state.calendarProjectHours,
            onApply = { projects ->
                onIntent(SetCalendarProjects(projects))
                showProjectFilter = false
            },
            onDismiss = { showProjectFilter = false },
        )
    }

    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val settleSpec: AnimationSpec<Float> = remember(animationsEnabled) {
        if (animationsEnabled) {
            spring(
                dampingRatio = 0.82f,
                stiffness = 420f,
            )
        } else {
            snap()
        }
    }
    val gestureThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = expansionState,
        positionalThreshold = { distance -> minOf(distance * 0.5f, gestureThresholdPx) },
        animationSpec = settleSpec,
    )

    val dayWorks = remember(state.calendarDays) {
        state.calendarDays.map { DayWork(it.date, it.time / 3600.0) }
    }
    LaunchedEffect(state.calendarMonth, requestedMonth, animationsEnabled) {
        if (requestedMonth == state.calendarMonth) {
            if (animationsEnabled) delay(MONTH_TRANSITION_DURATION_MS)
            requestedMonth = null
            monthTransitioning = false
        }
    }

    fun requestMonthChange(month: java.time.YearMonth) {
        if (
            !isCalendarFullyExpanded(expansionState) ||
            state.isCalendarLoading ||
            monthTransitioning ||
            requestedMonth != null
        ) return

        requestedMonth = month
        monthTransitioning = animationsEnabled
        onIntent(ChangeCalendarMonth(month))
    }
    val monthColors = rememberHomeMonthColors()

    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home-header"),
    ) { constraints ->
        val maxHeight = availableHeightPx
            .coerceAtLeast(1)
            .coerceAtMost(constraints.maxHeight)
        val contentConstraints = constraints.copy(minHeight = 0, maxHeight = maxHeight)

        val collapsedPlaceable = subcompose("collapsed-header") {
            HomeHeaderSurface(
                state = state,
                progress = 0f,
                onToggleCalendar = onToggleCalendar,
                onCloseCalendar = onCloseCalendar,
                onExport = { showMonthPicker = true },
                onAddWork = onAddWork,
                monthColors = monthColors,
                dayWorks = dayWorks,
                selectedCalendarDate = selectedCalendarDate,
                onSelectDay = {},
                onSelectCalendarDate = {},
                onPrevMonth = {},
                onNextMonth = {},
                onViewMonth = {},
                onShowProjectFilter = {},
                showCalendar = false,
                isCalendarFullyExpanded = false,
                monthTransitioning = false,
                animationsEnabled = animationsEnabled,
                modifier = Modifier,
                dragModifier = Modifier,
            )
        }.first().measure(contentConstraints)

        val collapsedHeight = collapsedPlaceable.height.coerceAtLeast(1)
        val expansionDistance = (maxHeight - collapsedHeight).coerceAtLeast(0)
        expansionState.updateAnchors(
            DraggableAnchors {
                CalendarExpansion.Collapsed at 0f
                CalendarExpansion.Expanded at expansionDistance.toFloat()
            },
        )

        val currentOffset = expansionState.offset.takeUnless { it.isNaN() }
            ?: if (
                expansionState.currentValue == CalendarExpansion.Expanded ||
                expansionState.targetValue == CalendarExpansion.Expanded
            ) {
                expansionDistance.toFloat()
            } else {
                0f
            }
        val headerHeight = (collapsedHeight + currentOffset)
            .coerceIn(collapsedHeight.toFloat(), maxHeight.toFloat())
            .roundToInt()
        val actualProgress = if (expansionDistance == 0) 1f else {
            (currentOffset / expansionDistance).coerceIn(0f, 1f)
        }

        val contentPlaceable = subcompose("calendar-header") {
            HomeHeaderSurface(
                state = state,
                progress = actualProgress,
                onToggleCalendar = onToggleCalendar,
                onCloseCalendar = onCloseCalendar,
                onExport = { showMonthPicker = true },
                onAddWork = onAddWork,
                monthColors = monthColors,
                dayWorks = dayWorks,
                selectedCalendarDate = selectedCalendarDate,
                onSelectDay = { day -> onIntent(SelectDay(day)) },
                onSelectCalendarDate = { date ->
                    onCalendarDateSelected(date)
                    navigator.openMonth(date.year, date.monthValue, date.dayOfMonth)
                },
                onPrevMonth = {
                    requestMonthChange(state.calendarMonth.minusMonths(1))
                },
                onNextMonth = {
                    requestMonthChange(state.calendarMonth.plusMonths(1))
                },
                onViewMonth = {
                    navigator.openMonth(state.calendarMonth.year, state.calendarMonth.monthValue, 1)
                },
                onShowProjectFilter = { showProjectFilter = true },
                showCalendar = true,
                isCalendarFullyExpanded = isCalendarFullyExpanded(expansionState, actualProgress),
                monthTransitioning = monthTransitioning || state.isCalendarLoading,
                animationsEnabled = animationsEnabled,
                modifier = Modifier,
                dragModifier = Modifier
                    .anchoredDraggable(
                        state = expansionState,
                        orientation = Orientation.Vertical,
                        flingBehavior = flingBehavior,
                    ),
            )
        }.first().measure(contentConstraints)

        layout(constraints.maxWidth, headerHeight) {
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun HomeHeaderSurface(
    state: HomeState,
    progress: Float,
    onToggleCalendar: () -> Unit,
    onCloseCalendar: () -> Unit,
    onExport: () -> Unit,
    onAddWork: () -> Unit,
    monthColors: MonthViewColors,
    dayWorks: List<DayWork>,
    selectedCalendarDate: LocalDate?,
    onSelectDay: (Int) -> Unit,
    onSelectCalendarDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewMonth: () -> Unit,
    onShowProjectFilter: () -> Unit,
    showCalendar: Boolean,
    isCalendarFullyExpanded: Boolean,
    monthTransitioning: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier,
    dragModifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val collapsedLabel = stringResource(R.string.calendar_closed_state)
    val expandedLabel = stringResource(R.string.calendar_open_state)
    val actionLabel = stringResource(
        if (progress > 0.5f) R.string.close_calendar_action else R.string.open_calendar_action,
    )
    val selectedDate = remember(state.dayPerMonth, state.selectedDay) {
        state.dayPerMonth.firstOrNull { it.date.dayOfMonth == state.selectedDay }?.date
            ?: LocalDate.now()
    }
    val showCalendarHeader = showCalendar && progress >= 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showCalendar) Modifier.fillMaxSize() else Modifier)
            .clip(MaterialTheme.shapes.large)
            .background(colors.primary)
            .then(dragModifier)
            .semantics {
                stateDescription = if (progress > 0.5f) expandedLabel else collapsedLabel
                customActions = listOf(CustomAccessibilityAction(actionLabel) {
                    onToggleCalendar()
                    true
                })
            },
    ) {
        if (showCalendarHeader) {
            CalendarHeaderBase(
                onCloseCalendar = onCloseCalendar,
                onShowProjectFilter = onShowProjectFilter,
                onViewMonth = onViewMonth,
                hasActiveProjectFilter = state.calendarSelectedProjects.isNotEmpty(),
            )
        } else {
            HomeHeaderBase(
                state = state,
                selectedDate = selectedDate,
                progress = progress,
                onToggleCalendar = onToggleCalendar,
                onExport = onExport,
                onAddWork = onAddWork,
                onSelectDay = onSelectDay,
            )
        }

        if (showCalendar) {
            CalendarBody(
                progress = progress,
                monthColors = monthColors,
                calendarContent = CalendarContent(
                    month = state.calendarMonth,
                    days = dayWorks,
                    totalHours = state.calendarTotalHours,
                ),
                selectedDate = selectedCalendarDate,
                onSelectDate = onSelectCalendarDate,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onViewMonth = onViewMonth,
                isCalendarFullyExpanded = isCalendarFullyExpanded,
                monthTransitioning = monthTransitioning,
                animationsEnabled = animationsEnabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeHeaderBase(
    state: HomeState,
    selectedDate: LocalDate,
    progress: Float,
    onToggleCalendar: () -> Unit,
    onExport: () -> Unit,
    onAddWork: () -> Unit,
    onSelectDay: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    val selectedDayLabel = remember(selectedDate, locale) {
        selectedDate.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { character ->
                if (character.isLowerCase()) {
                    character.titlecase(locale)
                } else {
                    character.toString()
                }
            }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-header-drag-surface"),
        color = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    top = 4.dp,
                    bottom = 6.dp,
                ),
        ) {
            HomeHeaderBar(
                dayLabel = selectedDayLabel,
                onToggleCalendar = onToggleCalendar,
                onExport = onExport,
                onAddWork = onAddWork,
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )

            WeekHeader(
                selectedDate = selectedDate,
                monthDays = state.dayPerMonth,
                selectedDay = state.selectedDay,
                selectDay = onSelectDay,
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 2.dp,
                ),
            )

            PullHandle(
                progress = progress.coerceIn(0f, 1f),
                expanded = false,
                onClick = onToggleCalendar,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun CalendarHeaderBase(
    onCloseCalendar: () -> Unit,
    onShowProjectFilter: () -> Unit,
    onViewMonth: () -> Unit,
    hasActiveProjectFilter: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        CalendarHeaderBar(
            onBack = onCloseCalendar,
            onShowProjectFilter = onShowProjectFilter,
            onViewMonth = onViewMonth,
            hasActiveProjectFilter = hasActiveProjectFilter,
            enabled = true,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 6.dp)
                .fillMaxWidth()
                .height(64.dp),
        )
    }
}

@Composable
private fun HomeHeaderBar(
    dayLabel: String,
    onToggleCalendar: () -> Unit,
    onExport: () -> Unit,
    onAddWork: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .then(
                if (enabled) {
                    Modifier
                } else {
                    Modifier.clearAndSetSemantics {}
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(
                    enabled = enabled,
                    onClick = onToggleCalendar,
                )
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayLabel,
                style = typography.headlineSmall.copy(
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.width(4.dp))

//            Icon(
//                imageVector = Icons.Default.KeyboardArrowDown,
//                contentDescription = null,
//                tint = colors.onSurfaceVariant,
//                modifier = Modifier.size(22.dp),
//            )
        }

        IconButton(
            onClick = onExport,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = exportIcon(),
                contentDescription = stringResource(R.string.export_action),
                tint = colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(4.dp))

        IconButton(
            onClick = onAddWork,
            enabled = enabled,
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ),
        ) {
            Icon(
                imageVector = addIcon(),
                contentDescription = stringResource(R.string.add_hours_btn),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun CalendarHeaderBar(
    onBack: () -> Unit,
    onShowProjectFilter: () -> Unit,
    onViewMonth: () -> Unit,
    hasActiveProjectFilter: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = modifier.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onBack,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterStart)
                .testTag("calendar-header-back"),
        ) {
            Icon(
                imageVector = arrowLeftIcon(),
                contentDescription = stringResource(
                    R.string.close_calendar_action
                ),
                tint = colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = stringResource(R.string.calendar_title),
            style = typography.titleLarge.copy(
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            CalendarHeaderIconButton(
                onClick = onShowProjectFilter,
                icon = filterIcon(),
                contentDescription = stringResource(R.string.project_filter_cd),
                active = hasActiveProjectFilter,
                enabled = enabled,
                testTag = "calendar-header-filter",
            )
            CalendarHeaderIconButton(
                onClick = onViewMonth,
                icon = dateRangeIcon(),
                contentDescription = stringResource(R.string.project_date_cd),
                active = false,
                enabled = enabled,
                testTag = "calendar-header-month-view",
            )
        }
    }
}

@Composable
private fun CalendarHeaderIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean,
    testTag: String,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(48.dp)) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surfaceContainerHigh)
                .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.medium)
                .testTag(testTag),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(colors.primaryContainer),
            )
        }
    }
}

@Composable
private fun CalendarBody(
    progress: Float,
    monthColors: MonthViewColors,
    calendarContent: CalendarContent,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewMonth: () -> Unit,
    isCalendarFullyExpanded: Boolean,
    monthTransitioning: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val bodyAlpha = ((progress - 0.15f) / 0.4f).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val horizontalSwipeThresholdPx = with(density) {
        (MaterialTheme.spacing.extraLarge * 2).toPx()
    }
    val swipeModifier = Modifier.calendarMonthSwipeGesture(
        enabled = isCalendarFullyExpanded && !monthTransitioning,
        thresholdPx = horizontalSwipeThresholdPx,
        onPreviousMonth = onPrevMonth,
        onNextMonth = onNextMonth,
    )

    AnimatedContent(
        targetState = calendarContent,
        modifier = modifier
            .background(monthColors.background)
            .graphicsLayer {
                alpha = bodyAlpha
                translationY = (1f - progress) * 24.dp.toPx()
            }
            .testTag("calendar-month-content")
            .then(swipeModifier),
        transitionSpec = {
            monthTransitionSpec(
                forward = targetState.month.isAfter(initialState.month),
                enabled = animationsEnabled && targetState.month != initialState.month,
            )
        },
        label = "calendar-month-transition",
    ) { content ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            CalendarSelectorRow(
                monthYear = content.month,
                onClose = {},
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onViewMonth = onViewMonth,
                monthColors = monthColors,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val gridOffset = (content.month.atDay(1).dayOfWeek.value - 1)
                val weekCount = (gridOffset + content.month.lengthOfMonth() + 6) / 7
                val cellHeight = ((maxHeight - 120.dp) / weekCount.coerceAtLeast(4))
                    .coerceIn(48.dp, 72.dp)
                MonthGrid(
                    monthYear = content.month,
                    days = content.days,
                    selectedDate = selectedDate,
                    onSelectDate = onSelectDate,
                    cellHeight = cellHeight,
                    totalHours = content.totalHours,
                    monthColors = monthColors,
                )
            }
        }
    }
}

private const val MONTH_TRANSITION_DURATION_MS = 300L

private data class CalendarContent(
    val month: java.time.YearMonth,
    val days: List<DayWork>,
    val totalHours: Double,
)

private fun monthTransitionSpec(
    forward: Boolean,
    enabled: Boolean,
): ContentTransform {
    if (!enabled) return ContentTransform(EnterTransition.None, ExitTransition.None)

    val enterOffset: (Int) -> Int = if (forward) {
        { width -> width }
    } else {
        { width -> -width }
    }
    val exitOffset: (Int) -> Int = if (forward) {
        { width -> -width }
    } else {
        { width -> width }
    }
    return ContentTransform(
        targetContentEnter = fadeIn(tween(MONTH_TRANSITION_DURATION_MS.toInt())) +
            slideInHorizontally(
                animationSpec = tween(MONTH_TRANSITION_DURATION_MS.toInt()),
                initialOffsetX = enterOffset,
            ),
        initialContentExit = fadeOut(tween(MONTH_TRANSITION_DURATION_MS.toInt())) +
            slideOutHorizontally(
                animationSpec = tween(MONTH_TRANSITION_DURATION_MS.toInt()),
                targetOffsetX = exitOffset,
            ),
    )
}

private fun isCalendarFullyExpanded(
    expansionState: AnchoredDraggableState<CalendarExpansion>,
): Boolean = expansionState.currentValue == CalendarExpansion.Expanded &&
    expansionState.targetValue == CalendarExpansion.Expanded

private fun isCalendarFullyExpanded(
    expansionState: AnchoredDraggableState<CalendarExpansion>,
    progress: Float,
): Boolean = progress >= 1f && isCalendarFullyExpanded(expansionState)

@Composable
private fun PullHandle(
    progress: Float,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .width(64.dp)
            .height(48.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .testTag("home-pull-handle"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(colors.onPrimary.copy(alpha = 0.52f)),
            )
        }
    }
}

@Composable
private fun WeekHeader(
    selectedDate: LocalDate,
    selectedDay: Int,
    monthDays: List<DayModel>,
    selectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dayMap = remember(monthDays) { monthDays.associateBy { it.date } }
    val weekDates = remember(selectedDate) {
        val start = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
        List(7) { start.plusDays(it.toLong()) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxWidth < 380.dp || fontScale > 1.15f
        val gap = if (maxWidth < 340.dp || fontScale > 1.35f) 4.dp else if (compact) 5.dp else 7.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            weekDates.forEach { date ->
                val model = dayMap[date]
                val isSelected = date.dayOfMonth == selectedDay && model != null
                WeekDayChip(
                    date = date,
                    dayLabel = model?.day ?: weekdayLabel(date),
                    hoursText = model?.let { formatHoursFromSeconds(it.time) }.orEmpty(),
                    isSelected = isSelected,
                    enabled = model != null,
                    compact = compact,
                    onClick = { selectDay(date.dayOfMonth) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeekDayChip(
    date: LocalDate,
    dayLabel: String,
    hoursText: String,
    isSelected: Boolean,
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val content = when {
        isSelected -> colors.onPrimaryContainer
        enabled -> colors.onPrimary
        else -> colors.onPrimary.copy(alpha = 0.42f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            if (compact) 2.dp else 3.dp,
            Alignment.CenterVertically
        ),
        modifier = modifier
            .height(if (compact) 72.dp else 80.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh.copy(
                    alpha = 0.34f
                )
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 2.dp, vertical = if (compact) 6.dp else 8.dp),
    ) {
        Text(
            text = if (compact) compactWeekday(dayLabel) else dayLabel.take(3),
            color = content,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium.copy(color = content),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (!compact || hoursText.isNotBlank() && hoursText != "0") {
            Text(
                text = hoursText,
                color = content,
                fontSize = if (compact) 10.sp else 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun rememberHomeMonthColors(): MonthViewColors {
    val colors = MaterialTheme.colorScheme
    return remember(colors) {
        MonthViewColors(
            background = colors.surface,
            backgroundGlow = colors.surfaceContainerLow,
            cardBackground = colors.surfaceContainer,
            chipBackground = colors.surfaceContainerHigh,
            border = colors.outlineVariant.copy(alpha = 0.65f),
            divider = colors.outlineVariant.copy(alpha = 0.35f),
            text = colors.onPrimary,
            mutedText = colors.onSurfaceVariant,
            accent = colors.secondary,
            accentMuted = colors.primary.copy(alpha = 0.65f),
        )
    }
}

private fun weekdayLabel(date: LocalDate): String =
    listOf("Man", "Tir", "Ons", "Tor", "Fre", "L\u00f8r", "S\u00f8n")[date.dayOfWeek.value - 1]

private fun compactWeekday(label: String): String = label.take(2)
