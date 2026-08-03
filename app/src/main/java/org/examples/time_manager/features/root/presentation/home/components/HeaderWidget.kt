package org.examples.time_manager.features.root.presentation.home.components

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.time.LocalDate
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.features.calendar.presentation.components.CalendarSelectorRow
import org.examples.time_manager.features.calendar.presentation.components.CategorySegments
import org.examples.time_manager.features.calendar.presentation.components.DayWork
import org.examples.time_manager.features.calendar.presentation.components.MonthGrid
import org.examples.time_manager.features.calendar.presentation.components.MonthViewColors
import org.examples.time_manager.features.calendar.presentation.components.WeekdayRow
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.HomeState
import org.examples.time_manager.features.root.data.RootScreenEvents.CreateExcelDocumentEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkStateEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.SelectDayEvent
import org.examples.time_manager.features.root.presentation.home.CalendarExpansion
import org.examples.time_manager.features.root.presentation.home.MonthPickerDialog
import org.examples.time_manager.features.utils.formatHoursFromSeconds
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.addIcon
import org.examples.time_manager.ui.theme.arrowDownIcon
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.closeIcon
import org.examples.time_manager.ui.theme.exportIcon
import java.time.format.TextStyle

@Composable
fun HeaderWidget(
    state: HomeState,
    vm: HomeViewModel,
    navigator: Navigator,
    expansionState: AnchoredDraggableState<CalendarExpansion>,
    availableHeightPx: Int,
    selectedCalendarDate: LocalDate?,
    onCalendarDateSelected: (LocalDate) -> Unit,
    onToggleCalendar: () -> Unit,
    onAddWork: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    val months = stringArrayResource(R.array.months_array).toList()
    val context = LocalContext.current

    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val month = result.data?.getStringExtra("stringKey")?.toIntOrNull()
                result.data?.data?.let { uri ->
                    vm.onEvent(CreateExcelDocumentEvent(context, uri, month))
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
            onEvent = vm::onEvent,
            projectValues = state.projects,
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
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = expansionState,
        positionalThreshold = { distance -> distance * 0.5f },
        animationSpec = settleSpec,
    )

    val dayWorks = remember(state.calendarDays) {
        state.calendarDays.map { DayWork(it.date, it.time / 3600.0) }
    }
    val monthColors = rememberHomeMonthColors()
    val density = LocalDensity.current

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
                expandedHeight = 0.dp,
                expansionState = expansionState,
                onToggleCalendar = onToggleCalendar,
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
                onSelectProject = {},
                onClearProjects = {},
                showCalendar = false,
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

        val currentOffset = expansionState.offset.takeUnless { it.isNaN() } ?: 0f
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
                expandedHeight = with(density) { expansionDistance.toDp() },
                expansionState = expansionState,
                onToggleCalendar = onToggleCalendar,
                onExport = { showMonthPicker = true },
                onAddWork = onAddWork,
                monthColors = monthColors,
                dayWorks = dayWorks,
                selectedCalendarDate = selectedCalendarDate,
                onSelectDay = { day -> vm.onEvent(SelectDayEvent(day)) },
                onSelectCalendarDate = { date ->
                    onCalendarDateSelected(date)
                    navigator.toMonthView(
                        date = date.withDayOfMonth(1).toString(),
                        day = date.dayOfMonth,
                    )
                },
                onPrevMonth = {
                    vm.onEvent(
                        org.examples.time_manager.features.root.data.RootScreenEvents.ChangeCalendarMonthEvent(
                            state.calendarMonth.minusMonths(1),
                        ),
                    )
                },
                onNextMonth = {
                    vm.onEvent(
                        org.examples.time_manager.features.root.data.RootScreenEvents.ChangeCalendarMonthEvent(
                            state.calendarMonth.plusMonths(1),
                        ),
                    )
                },
                onViewMonth = {
                    navigator.toMonthView(state.calendarMonth.atDay(1).toString(), 1)
                },
                onSelectProject = { project ->
                    vm.onEvent(
                        org.examples.time_manager.features.root.data.RootScreenEvents.ToggleCalendarProjectEvent(
                            project
                        ),
                    )
                },
                onClearProjects = {
                    vm.onEvent(
                        org.examples.time_manager.features.root.data.RootScreenEvents.ClearCalendarProjectsEvent,
                    )
                },
                showCalendar = true,
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
    expandedHeight: Dp,
    expansionState: AnchoredDraggableState<CalendarExpansion>,
    onToggleCalendar: () -> Unit,
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
    onSelectProject: (Project) -> Unit,
    onClearProjects: () -> Unit,
    showCalendar: Boolean,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(colors.primary)
            .semantics {
                stateDescription = if (progress > 0.5f) expandedLabel else collapsedLabel
                customActions = listOf(CustomAccessibilityAction(actionLabel) {
                    onToggleCalendar()
                    true
                })
            },
    ) {
        HeaderBase(
            state = state,
            selectedDate = selectedDate,
            progress = progress,
            onToggleCalendar = onToggleCalendar,
            onExport = onExport,
            onAddWork = onAddWork,
            onSelectDay = onSelectDay,
            expansionState = expansionState,
            modifier = dragModifier,
        )

        if (showCalendar) {
            CalendarBody(
                state = state,
                progress = progress,
                expandedHeight = expandedHeight,
                monthColors = monthColors,
                dayWorks = dayWorks,
                selectedDate = selectedCalendarDate,
                onSelectDate = onSelectCalendarDate,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onViewMonth = onViewMonth,
                onSelectProject = onSelectProject,
                onClearProjects = onClearProjects,
            )
        }
    }
}

@Composable
private fun HeaderBase(
    state: HomeState,
    selectedDate: LocalDate,
    progress: Float,
    onToggleCalendar: () -> Unit,
    onExport: () -> Unit,
    onAddWork: () -> Unit,
    onSelectDay: (Int) -> Unit,
    expansionState: AnchoredDraggableState<CalendarExpansion>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    val clampedProgress = progress.coerceIn(0f, 1f)

    /*
     * Kalender-topplinjen kommer inn sent i overgangen.
     * Ukesstripen forsvinner tidligere, slik at overgangen
     * ikke blir visuelt overfylt.
     */
    val calendarBarProgress = FastOutSlowInEasing.transform(
        clampedProgress.segment(start = 0.68f, end = 0.92f)
    )

    val weekVisibility = 1f - FastOutSlowInEasing.transform(
        clampedProgress.segment(start = 0.28f, end = 0.70f)
    )

    val handleVisibility = 1f - FastOutSlowInEasing.transform(
        clampedProgress.segment(start = 0.58f, end = 0.88f)
    )

    val expanded =
        expansionState.targetValue == CalendarExpansion.Expanded ||
                clampedProgress >= 0.5f

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
        modifier = modifier
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
            /*
             * Begge topplinjene ligger på samme sted.
             * De crossfades etter drag-progresjonen.
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                HomeHeaderBar(
                    dayLabel = selectedDayLabel,
                    onToggleCalendar = onToggleCalendar,
                    onExport = onExport,
                    onAddWork = onAddWork,
                    enabled = calendarBarProgress < 0.5f,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = 1f - calendarBarProgress
                            translationY =
                                -6.dp.toPx() * calendarBarProgress
                        },
                )

                CalendarHeaderBar(
                    onBack = onToggleCalendar,
                    onTrailingAction = onExport,
                    enabled = calendarBarProgress >= 0.5f,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = calendarBarProgress
                            translationY =
                                6.dp.toPx() * (1f - calendarBarProgress)
                        },
                )
            }

            /*
             * Ikke bruk bare alpha her. Da vil WeekHeader fortsatt
             * oppta plass etter at den har blitt usynlig.
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .collapseVertically(weekVisibility)
                    .clipToBounds()
                    .graphicsLayer {
                        alpha = weekVisibility
                        translationY =
                            -8.dp.toPx() * (1f - weekVisibility)
                    },
            ) {
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
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .collapseVertically(handleVisibility)
                    .clipToBounds()
                    .graphicsLayer {
                        alpha = handleVisibility
                    },
            ) {
                PullHandle(
                    progress = clampedProgress,
                    expanded = expanded,
                    onClick = onToggleCalendar,
                    modifier = Modifier.padding(
                        top = 10.dp,
                        bottom = 4.dp,
                    ),
                )
            }
        }
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
                .clip(RoundedCornerShape(14.dp))
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

        FilledIconButton(
            onClick = onAddWork,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
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
    onTrailingAction: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .then(
                if (enabled) {
                    Modifier
                } else {
                    Modifier.clearAndSetSemantics {}
                }
            ),
    ) {
        IconButton(
            onClick = onBack,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
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
            modifier = Modifier.align(Alignment.Center),
        )

        IconButton(
            onClick = onTrailingAction,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp),
        ) {
            Icon(
                imageVector = exportIcon(),
                contentDescription = stringResource(R.string.export_action),
                tint = colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Returnerer 0–1 for en bestemt del av den totale progresjonen.
 */
private fun Float.segment(
    start: Float,
    end: Float,
): Float {
    if (end <= start) return 0f

    return ((this - start) / (end - start))
        .coerceIn(0f, 1f)
}

/**
 * Komprimerer faktisk layout-høyde, ikke bare synligheten.
 *
 * Innholdet forsvinner oppover mens høyden reduseres.
 */
private fun Modifier.collapseVertically(
    visibleFraction: Float,
): Modifier {
    val fraction = visibleFraction.coerceIn(0f, 1f)

    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val visibleHeight =
            (placeable.height * fraction).roundToInt()

        layout(
            width = placeable.width,
            height = visibleHeight,
        ) {
            placeable.placeRelative(
                x = 0,
                y = visibleHeight - placeable.height,
            )
        }
    }
}

@Composable
private fun CalendarBody(
    state: HomeState,
    progress: Float,
    expandedHeight: Dp,
    monthColors: MonthViewColors,
    dayWorks: List<DayWork>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewMonth: () -> Unit,
    onSelectProject: (Project) -> Unit,
    onClearProjects: () -> Unit,
) {
    val bodyAlpha = ((progress - 0.15f) / 0.4f).coerceIn(0f, 1f)
    val totalHours = remember(dayWorks) { dayWorks.sumOf { it.hours } }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(monthColors.background)
            .graphicsLayer {
                alpha = bodyAlpha
                translationY = (1f - progress) * 24.dp.toPx()
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalendarSelectorRow(
            monthYear = state.calendarMonth,
            onClose = {},
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onViewMonth = onViewMonth,
            monthColors = monthColors,
        )
        CategorySegments(
            projects = state.projects.collectAsState(initial = emptyList()).value,
            selectedProjects = state.calendarSelectedProjects,
            onSelectProject = onSelectProject,
            onClearSelection = onClearProjects,
            monthColors = monthColors,
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridOffset = (state.calendarMonth.atDay(1).dayOfWeek.value - 1)
            val weekCount = (gridOffset + state.calendarMonth.lengthOfMonth() + 6) / 7
            val cellHeight = ((maxHeight - 250.dp) / weekCount.coerceAtLeast(4))
                .coerceIn(48.dp, 68.dp)
            MonthGrid(
                monthYear = state.calendarMonth,
                days = dayWorks,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                cellHeight = cellHeight,
                totalHours = totalHours,
                monthColors = monthColors,
            )
        }
    }
}

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
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
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
            .clip(RoundedCornerShape(if (isSelected) 22.dp else 18.dp))
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
