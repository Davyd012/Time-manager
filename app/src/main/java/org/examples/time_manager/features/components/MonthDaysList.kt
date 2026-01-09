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
import org.examples.time_manager.features.utils.formatHoursFromSeconds

@SuppressLint("DefaultLocale")
@Composable
fun MonthDaysList(
    listState: LazyListState,
    selectDay: (Int) -> Unit,
    selectedDay: Int,
    monthDays: List<DayModel>,
) {
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    LazyRow(
        state = listState,
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
//            .background(colors.primaryContainer)
            .padding(5.dp),
    ) {
//        item {
//            Text(monthDays.size.toString())
//        }
        items(monthDays.size) { index ->
            val it = monthDays.elementAt(index)
            val currentDay = it.date.dayOfMonth == selectedDay

            val textColor =
                if (currentDay) colors.onPrimaryContainer else colors.onPrimary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (currentDay) colors.primaryContainer else colors.primary)
                    .clickable { selectDay(index) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(it.day.substring(0, 3), color = textColor)
                Text(
                    it.date.dayOfMonth.toString(),
                    style = style.titleMedium.copy(color = textColor)
                )
                Text(formatHoursFromSeconds(it.time), color = textColor)
            }
        }
    }
}