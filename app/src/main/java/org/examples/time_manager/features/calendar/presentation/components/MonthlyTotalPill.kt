package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.examples.time_manager.R

@Composable
fun MonthlyTotalPill(
    totalHours: Double,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val prefix = stringResource(R.string.total_month_prefix)
    val label =
        buildAnnotatedString {
            append(prefix.format(totalHours))
            pushStyle(SpanStyle(color = colors.onPrimary, fontWeight = FontWeight.SemiBold))
            pop()
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.primary.copy(alpha = .8f),
        border = BorderStroke(1.dp, colors.onPrimary.copy(alpha = 0.4f)),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = colors.onPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
