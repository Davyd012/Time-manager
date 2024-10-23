package org.examples.time_manager.features.root.presentation.home.preview

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import org.examples.time_manager.features.root.HomeState
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.SelectDayEvent
import org.examples.time_manager.ui.theme.LightColorScheme

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthDaysList(
    listState: LazyListState,
    state: HomeState,
    vm: HomeViewModel,
) {
    val style = MaterialTheme.typography
    val colors = LightColorScheme

    LazyRow(
        state = listState,
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
//            .background(colors.primaryContainer)
            .padding(5.dp),
    ) {
        item {
            Text(state.dayPerMonth.size.toString())
        }
        items(state.dayPerMonth.size) { index ->
            val it = state.dayPerMonth.elementAt(index)
//            val currentDay = it.date == state.selectedDay
            val currentDay = index == 1
            var text = "0"
            if (it.hours > 0) text += it.hours / 60
            if (it.hours % 60 > 0) text += "." + it.hours % 60

            val textColor =
                if (currentDay) colors.onSecondaryContainer else colors.onPrimary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (currentDay) colors.primaryContainer else colors.primary)
                    .clickable { vm.onEvent(SelectDayEvent(index + 1)) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(it.day.substring(0, 3), color = textColor)
                Text(
                    it.date.toString(),
                    style = style.titleMedium.copy(color = textColor)
                )
                Text(text, color = textColor)
            }
        }
    }
}