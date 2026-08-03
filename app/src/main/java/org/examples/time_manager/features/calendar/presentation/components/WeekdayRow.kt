package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekdayRow(
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography
    val labels = rememberWeekdayLabels()

    Row(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 15.dp)
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = texts.labelLarge.copy(
                    color = colors.onSurface.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium,
                )
            )
        }
    }
}

@Composable
fun rememberWeekdayLabels(): List<String> {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    return remember(locale) {
        DayOfWeek.entries.map { day ->
            day.getDisplayName(TextStyle.SHORT, locale)
                .removeSuffix(".")
                .replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(locale)
                    } else {
                        char.toString()
                    }
                }
        }
    }
}