package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    val shape = RoundedCornerShape(18.dp)
    val currentTitle = formatMonthTitle(monthYear, "MMMM yyyy")
    val prevTitle = formatMonthTitle(monthYear.minusMonths(1), "MMM yyyy")
    val nextTitle = formatMonthTitle(monthYear.plusMonths(1), "MMM yyyy")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            /* Previous month text */
            Surface(
                onClick = onPrevMonth,
                color = Color.Transparent,
                shape = shape,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = prevTitle,
                    style = texts.bodyMedium,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }

            /* Center controls */
            Row(
                modifier = Modifier.weight(2.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {

                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = arrowLeftIcon(),
                        contentDescription = stringResource(R.string.previous_month_cd),
                        tint = colors.onSurface.copy(alpha = 0.85f),
                    )
                }

                val interactionSource = remember { MutableInteractionSource() }

                Text(
                    text = currentTitle,
                    color = colors.onSurface,
                    style = texts.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false),
                        ) { onViewMonth() },
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = arrowRightIcon(),
                        contentDescription = stringResource(R.string.next_month_cd),
                        tint = colors.onSurface.copy(alpha = 0.85f),
                    )
                }
            }

            /* Next month text */
            Surface(
                onClick = onNextMonth,
                color = Color.Transparent,
                shape = shape,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = nextTitle,
                    style = texts.bodyMedium,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun formatMonthTitle(monthYear: YearMonth, pattern: String): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale("nb", "NO"))
    return monthYear.format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("nb", "NO")) else it.toString()
    }
}
