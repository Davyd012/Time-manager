package org.examples.time_manager.features.calendar.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.R
import org.examples.time_manager.ui.theme.shareIcon
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthGrid(
    monthYear: YearMonth,
    days: List<DayWork>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 68.dp,
    totalHours: Int,
) {
    val colors = MaterialTheme.colorScheme
    val monthViewColors = MonthViewColors.defaults()
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

    val itemRows = gridCells.chunked(7)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = colors.surfaceContainer,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, colors.onPrimary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemRows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { cell ->
                        DayCell(
                            cell = cell,
                            dayWork = cell?.let { dayMap[it].orEmpty() } ?: emptyList(),
                            selectedDate = selectedDate,
                            onSelectDate = onSelectDate,
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight),
                        )
                    }

                    repeat(7 - row.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                        )
                    }
                }

                if (index < itemRows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth(), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MonthlyTotalPill(
                        totalHours = totalHours,
                        colors = monthViewColors,
                    )
                    ShareMonthButton(
                        monthYear = monthYear,
                        totalHours = totalHours,
                        colors = monthViewColors,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ShareMonthButton(
    monthYear: YearMonth,
    totalHours: Int,
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shareText =
        remember(monthYear, totalHours) {
            buildMonthShareText(context, monthYear, totalHours)
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.9f)),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        IconButton(
            onClick = { shareMonthSummary(context, shareText) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = shareIcon(),
                contentDescription = stringResource(R.string.share_month_cd),
                tint = colors.accent,
            )
        }
    }
}

@Composable
private fun DayCell(
    cell: LocalDate?,
    dayWork: List<DayWork>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
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
                    color = colors.onPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hours.toString(),
                    color =
                        if (hours == 0) {
                            colors.onPrimary.copy(alpha = 0.45f)
                        } else {
                            colors.onPrimaryContainer
                        },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (isSelected) {
                SelectedUnderline(
                    color = colors.onPrimary,
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

private fun buildMonthShareText(context: Context, monthYear: YearMonth, totalHours: Int): String {
    val locale = Locale("nb", "NO")
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val monthLabel = monthYear.format(formatter).lowercase(locale)
    val hoursLabel = if (totalHours == 1) context.getString(R.string.hour_singular) else context.getString(R.string.hour_plural)

    return when {
        totalHours <= 0 ->
            context.getString(R.string.summary_no_hours, monthLabel)
        totalHours < 20 ->
            context.getString(R.string.summary_quiet_month, monthLabel, totalHours, hoursLabel)
        totalHours >= 160 ->
            context.getString(R.string.summary_active_month, totalHours, hoursLabel, monthLabel)
        else ->
            context.getString(R.string.summary_normal_month, totalHours, hoursLabel, monthLabel)
    }
}

private fun shareMonthSummary(context: Context, shareText: String) {
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
