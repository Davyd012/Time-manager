package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.domain.calendar.interpolateCalendarCoordinate
import no.timnor.app.shared.domain.calendar.monthGridDates
import no.timnor.app.shared.domain.calendar.monthGridDateRow
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.calendar.monthGridRow
import no.timnor.app.shared.domain.calendar.weekDates
import no.timnor.app.shared.domain.calendar.formatCalendarDayHours
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_hour_abbreviation
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

private data class CalendarCellGeometry(
    val date: LocalDate,
    val column: Int,
    val row: Int,
    val inMonth: Boolean,
    val selectedWeekDate: Boolean,
)

/**
 * One date-keyed set of nodes occupies both the week and month endpoints.
 * Selected-week cells move into their real month row; surrounding dates reveal
 * in place as the measured calendar grows.
 */
@Composable
internal fun CalendarMonthGrid(
    month: LocalDate,
    selectedDate: LocalDate,
    selectedGridDate: LocalDate?,
    secondsByDate: Map<LocalDate, Int>,
    weekSecondsByDate: Map<LocalDate, Int>,
    weekdays: List<String>,
    expansionProgress: Float,
    interactive: Boolean,
    onSelectWeekDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = expansionProgress.coerceIn(0f, 1f)
    val monthStyle = p >= 0.5f
    val monthNumber = month.month.ordinal + 1
    val year = month.year
    val gridDates = remember(year, monthNumber) { monthGridDates(year, monthNumber) }
    val selectedWeek = remember(selectedDate) { weekDates(selectedDate).toSet() }
    val dates = remember(gridDates, selectedWeek) {
        (gridDates + selectedWeek).distinct().sorted()
    }
    val rows = remember(year, monthNumber) {
        monthGridLayout(year, monthNumber).let {
            (it.leadingEmptyDays + it.daysInMonth + it.trailingEmptyDays) / 7
        }
    }
    val cells = remember(dates, gridDates, selectedWeek, year, monthNumber, rows) {
        dates.map { date ->
            CalendarCellGeometry(
                date = date,
                column = date.dayOfWeek.ordinal,
                row = monthGridDateRow(date, year, monthNumber) ?: rows + 1,
                inMonth = date.year == year && date.month.ordinal + 1 == monthNumber,
                selectedWeekDate = date in selectedWeek,
            )
        }
    }
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val collapsedCellHeight = MaterialTheme.timnorSpacing.extraLarge * 2 * fontScale
    val expandedCellHeight = MaterialTheme.timnorSpacing.extraLarge * 2 * fontScale
    val weekdayHeight = MaterialTheme.timnorSpacing.extraLarge * fontScale
    val weekdayHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { weekdayHeight.roundToPx() }
    val collapsedCellHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { collapsedCellHeight.roundToPx() }
    val expandedCellHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { expandedCellHeight.roundToPx() }
    val columnSpacingPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (MaterialTheme.timnorSpacing.extraSmall * (1f - p)).roundToPx()
    }
    val selectedMonthRow = remember(selectedDate, year, monthNumber, rows) {
        monthGridRow(selectedDate, year, monthNumber) ?: rows + 1
    }

    Layout(
        modifier = modifier.fillMaxWidth().clipToBounds().testTag("calendar-morph-grid"),
        content = {
            weekdays.forEachIndexed { index, weekday ->
                key("weekday-$index") {
                    Box(
                        Modifier.testTag("calendar-weekday-label-$index")
                            .background(MaterialTheme.colorScheme.primary)
                            .then(if (p > 0.01f) Modifier else Modifier.clearAndSetSemantics {}),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = weekday.take(3),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
            cells.forEach { cell ->
                key(cell.date) {
                    val cellClick = remember(cell.date, monthStyle, onSelectWeekDate, onSelectDate) {
                        {
                            if (monthStyle) onSelectDate(cell.date) else onSelectWeekDate(cell.date)
                        }
                    }
                    MorphCalendarDateCell(
                        date = cell.date,
                        weekday = legacyHomeWeekdayLabel(
                            date = cell.date,
                            selectedDate = selectedDate,
                            localizedWeekday = weekdays[cell.column],
                        ),
                        weekdayAlpha = if (cell.selectedWeekDate) {
                            (1f - (p / 0.15f).coerceIn(0f, 1f))
                        } else 0f,
                        seconds = if (!monthStyle) weekSecondsByDate[cell.date] ?: 0 else secondsByDate[cell.date] ?: 0,
                        selected = if (!monthStyle) selectedDate == cell.date
                        else selectedGridDate == cell.date || (selectedGridDate == null && selectedDate == cell.date),
                        inMonth = cell.inMonth,
                        selectedWeekDate = cell.selectedWeekDate,
                        visible = when {
                            cell.selectedWeekDate && cell.row >= rows -> p < 0.999f
                            cell.selectedWeekDate -> true
                            !cell.inMonth -> false
                            else -> p > 0.01f || cell.selectedWeekDate
                        },
                        enabled = if (!monthStyle) {
                            cell.selectedWeekDate && cell.date.year == selectedDate.year && cell.date.month == selectedDate.month
                        } else interactive && cell.inMonth,
                        monthStyle = monthStyle,
                        onClick = cellClick,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val availableWidth = (width - columnSpacingPx * 6).coerceAtLeast(7)
        val cellWidth = availableWidth / 7
        val remainder = availableWidth % 7
        val currentCellHeightPx = interpolateCalendarCoordinate(
            collapsedCellHeightPx.toFloat(),
            expandedCellHeightPx.toFloat(),
            p,
        ).toInt()
        val height = interpolateCalendarCoordinate(
            collapsedCellHeightPx.toFloat(),
            (weekdayHeightPx + rows * expandedCellHeightPx).toFloat(),
            p,
        ).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeables = measurables.drop(7).mapIndexed { index, measurable ->
            val cell = cells[index]
            val col = cell.column
            val cellW = cellWidth + if (col < remainder) 1 else 0
            measurable.measure(Constraints.fixed(cellW, currentCellHeightPx))
        }
        val labels = measurables.take(7).mapIndexed { index, measurable ->
            val labelW = cellWidth + if (index < remainder) 1 else 0
            measurable.measure(Constraints.fixed(labelW, weekdayHeightPx))
        }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val cell = cells[index]
                val col = cell.column
                val x = col * cellWidth + minOf(col, remainder) + col * columnSpacingPx
                val targetY = weekdayHeightPx + cell.row * expandedCellHeightPx
                val selectedRowOrigin = interpolateCalendarCoordinate(
                    0f,
                    (weekdayHeightPx + selectedMonthRow * expandedCellHeightPx).toFloat(),
                    p,
                )
                val y = if (cell.selectedWeekDate) {
                    interpolateCalendarCoordinate(0f, targetY.toFloat(), p).toInt()
                } else {
                    (selectedRowOrigin + (cell.row - selectedMonthRow) * expandedCellHeightPx).toInt()
                }
                val shown = when {
                    cell.selectedWeekDate && cell.row >= rows -> 1f - p
                    cell.selectedWeekDate -> 1f
                    !cell.inMonth -> 0f
                    else -> p
                }
                placeable.placeWithLayer(x, y) { alpha = shown }
            }
            labels.forEachIndexed { index, placeable ->
                val x = index * cellWidth + minOf(index, remainder) + index * columnSpacingPx
                placeable.placeWithLayer(x, 0) { alpha = ((p - 0.20f) / 0.10f).coerceIn(0f, 1f) }
            }
        }
    }
}

@Composable
private fun MorphCalendarDateCell(
    date: LocalDate,
    weekday: String,
    weekdayAlpha: Float,
    seconds: Int,
    selected: Boolean,
    inMonth: Boolean,
    selectedWeekDate: Boolean,
    visible: Boolean,
    monthStyle: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxSize()
            .testTag(if (inMonth) "calendar-month-day-${date}" else "calendar-date-${date}" )
            .semantics { this.selected = selected }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
        shape = MaterialTheme.shapes.medium,
        color = when {
            selected -> colors.primaryContainer
            !monthStyle && selectedWeekDate -> colors.surfaceContainerLow
            !monthStyle -> colors.surface
            else -> colors.primary
        },
        contentColor = when {
            selected -> colors.onPrimaryContainer
            else -> colors.onSurface
        },
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = MaterialTheme.timnorSpacing.extraSmall), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(MaterialTheme.timnorSpacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    if (weekdayAlpha > 0f) {
                        Text(
                            text = weekday.take(3),
                            modifier = Modifier.alpha(weekdayAlpha),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
                Text(date.day.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                val hoursText = if (seconds > 0) {
                    "${formatCalendarDayHours(seconds.toLong())}${stringResource(Res.string.calendar_hour_abbreviation)}"
                } else "0"
                Text(
                    hoursText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
