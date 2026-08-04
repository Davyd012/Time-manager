package org.examples.time_manager.features.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.features.root.presentation.home.components.DayCell
import org.examples.time_manager.features.utils.formatHoursFromSeconds

@SuppressLint("DefaultLocale")
@Composable
fun MonthDaysList(
    listState: LazyListState,
    selectDay: (Int) -> Unit,
    selectedDay: Int,
    monthDays: List<DayModel>,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
//            .background(colors.primaryContainer)
            .padding(5.dp),
    ) {
//        item {
//            Text(monthDays.size.toString())
//        }
        items(monthDays.size) { index ->
            val item = monthDays.elementAt(index)
            val currentDay = item.date.dayOfMonth == selectedDay

            val textColor =
                if (currentDay) colors.onPrimaryContainer else colors.onPrimary
            DayCell(
                dayLabel = item.day.take(3),
                dayOfMonth = item.date.dayOfMonth,
                hoursText = formatHoursFromSeconds(item.time),
                isSelected = currentDay,
                onClick = { selectDay(index) },
                colors = colors,
                texts = style
            )
        }
    }
}
