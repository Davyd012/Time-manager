package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.examples.time_manager.R
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.arrowRightIcon
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarSelectorRow(
    monthYear: YearMonth,
    onClose: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewMonth: () -> Unit,
    modifier: Modifier = Modifier,
    monthColors: MonthViewColors,
) {
    val currentTitle = formatMonthTitle(monthYear, "MMMM yyyy")
    val prevTitle = formatMonthTitle(monthYear.minusMonths(1), "MMM yyyy")
    val nextTitle = formatMonthTitle(monthYear.plusMonths(1), "MMM yyyy")
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val compact = maxWidth < 360.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                MonthLabel(
                    text = prevTitle,
                    onClick = onPrevMonth,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.weight(if (compact) 1f else 2.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.size(48.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = arrowLeftIcon(),
                        contentDescription = stringResource(R.string.previous_month_cd),
                        tint = colors.onSurface,
                    )
                }

                Surface(
                    onClick = onViewMonth,
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = arrowRightIcon(),
                        contentDescription = stringResource(R.string.next_month_cd),
                        tint = colors.onSurface,
                    )
                }
            }

            if (!compact) {
                MonthLabel(
                    text = nextTitle,
                    onClick = onNextMonth,
                    endAligned = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MonthLabel(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    endAligned: Boolean = false,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (endAligned) androidx.compose.ui.text.style.TextAlign.End
            else androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

private fun formatMonthTitle(monthYear: YearMonth, pattern: String): String {
    val norwegian = Locale.Builder().setLanguage("nb").setRegion("NO").build()
    val formatter = DateTimeFormatter.ofPattern(pattern, norwegian)
    return monthYear.format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(norwegian) else it.toString()
    }
}
