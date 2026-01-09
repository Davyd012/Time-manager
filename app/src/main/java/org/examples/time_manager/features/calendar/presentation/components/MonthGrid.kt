package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthGrid(
    monthYear: YearMonth,
    days: List<DayWork>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 68.dp,
) {
    val dayMap by remember(days) {
        derivedStateOf {
            days.groupBy { it.date }.mapValues { entry -> entry.value }
        }
    }

    val gridCells by remember(monthYear) {
        derivedStateOf {
            buildMonthGrid(monthYear)
        }
    }

    val rows = gridCells.size / 7
    val totalHeight = cellHeight * rows
    val density = LocalDensity.current
    val horizontalLineColor = colors.border.copy(alpha = 0.12f)
    val verticalLineColor = colors.border.copy(alpha = 0.08f)
    val outerLineColor = colors.border.copy(alpha = 0.12f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .drawBehind {
                        val columns = 7
                        val cellWidth = size.width / columns
                        val cellHeightPx = size.height / rows
                        val horizontalStroke = Stroke(width = with(density) { 0.8.dp.toPx() })
                        val verticalStroke = Stroke(width = with(density) { 0.5.dp.toPx() })

                        for (column in 1 until columns) {
                            val x = cellWidth * column
                            drawLine(
                                color = verticalLineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = verticalStroke.width,
                            )
                        }

                        for (row in 1 until rows) {
                            val y = cellHeightPx * row
                            drawLine(
                                color = horizontalLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = horizontalStroke.width,
                            )
                        }

                        drawRect(
                            color = outerLineColor,
                            style = Stroke(width = with(density) { 1.dp.toPx() }),
                        )
                    }
                    .padding(4.dp),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                contentPadding = PaddingValues(0.dp),
            ) {
                items(gridCells) { cell ->
                    DayCell(
                        cell = cell,
                        dayWork = cell?.let { dayMap[it].orEmpty() } ?: emptyList(),
                        selectedDate = selectedDate,
                        onSelectDate = onSelectDate,
                        colors = colors,
                        modifier = Modifier.height(cellHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: LocalDate?,
    dayWork: List<DayWork>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
) {
    val hours =
        remember(dayWork) {
            if (cell == null) {
                0
            } else {
                dayWork.sumOf { it.hours }
            }
        }
    val isSelected = cell != null && cell == selectedDate

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (cell != null) {
                        Modifier.clickable { onSelectDate(cell) }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        if (cell != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = cell.dayOfMonth.toString(),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hours.toString(),
                    color =
                        if (hours == 0) {
                            colors.text.copy(alpha = 0.45f)
                        } else {
                            colors.accent.copy(alpha = 0.9f)
                        },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (isSelected) {
                SelectedUnderline(
                    color = colors.accent,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun buildMonthGrid(monthYear: YearMonth): List<LocalDate?> {
    val firstOfMonth = monthYear.atDay(1)
    val firstWeekday = firstOfMonth.dayOfWeek
    val startOffset = (firstWeekday.value + 6) % 7
    val daysInMonth = monthYear.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()

    repeat(startOffset) { cells.add(null) }
    for (day in 1..daysInMonth) {
        cells.add(monthYear.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}
