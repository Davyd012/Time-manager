package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.examples.time_manager.R
import java.util.Locale

@Composable
fun MonthlyTotalPill(
    totalHours: Double,
    modifier: Modifier = Modifier,
    monthColors: MonthViewColors,
) {
    val locale = Locale.Builder().setLanguage("nb").setRegion("NO").build()
    val formattedLabel = stringResource(R.string.total_month_prefix, totalHours)
    val value = String.format(locale, "%.0f", totalHours)
    val valueStart = formattedLabel.indexOf(value)
    val label =
        buildAnnotatedString {
            if (valueStart >= 0) {
                append(formattedLabel.substring(0, valueStart))
                withStyle(SpanStyle(color = monthColors.accent, fontWeight = FontWeight.SemiBold)) {
                    append(value)
                }
                append(formattedLabel.substring(valueStart + value.length))
            } else {
                append(formattedLabel)
            }
        }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = monthColors.chipBackground.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, monthColors.border),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
