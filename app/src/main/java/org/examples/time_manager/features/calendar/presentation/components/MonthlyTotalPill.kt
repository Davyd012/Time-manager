package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.examples.time_manager.R
import java.util.Locale

@Composable
fun MonthlyTotalPill(
    totalHours: Double,
    modifier: Modifier = Modifier,
    monthColors: MonthViewColors = MonthViewColors.defaults(),
) {
    val prefix = stringResource(R.string.total_month_prefix)
    val value = String.format(Locale("nb", "NO"), "%.2fh", totalHours)
    val label =
        buildAnnotatedString {
            append(prefix.substringBefore("%1$.2fh"))
            withStyle(SpanStyle(color = monthColors.accent, fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = monthColors.chipBackground.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, monthColors.border),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
//            color = monthColors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
