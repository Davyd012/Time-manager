package org.examples.time_manager.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.examples.time_manager.features.calendar.utils.formatHoursFromSeconds

@Composable
fun CalendarScreen(vm: CalendarViewModel) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    val firstDay = (state.dayPerMonth.firstOrNull()?.date?.dayOfWeek?.ordinal
        ?: 0)
    val month by remember {
        mutableIntStateOf(
            (state.dayPerMonth.firstOrNull()?.date?.month?.ordinal
                ?: 0)
        )
    }
    val days =
        List(firstDay) { null } + (1..(state.dayPerMonth.size.takeIf { it > 0 } ?: 2)).toList()


    val months = remember {
        listOf(
            "Januar", "Februar", "Mars", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Desember"
        )
    }

    Column(
        modifier = Modifier
            .background(colors.primary)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = colors.onPrimary
            )
            Text(
                months.elementAt(month),
                style = texts.headlineMedium.copy(
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onPrimary
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.padding(16.dp)
        ) {
            items(days.size) { value ->
                if (days.elementAt(value) == null) return@items

                if (state.dayPerMonth.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(4.dp)
//                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .wrapContentHeight(unbounded = true)
                            .background(colors.primaryContainer, shape = RoundedCornerShape(4.dp))
                            .padding(10.dp)
                    ) {
                        val day = state.dayPerMonth.elementAt(value - firstDay)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                day.date.dayOfMonth.toString(),
                                style = texts.titleMedium.copy(color = colors.onPrimaryContainer)
                            )
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .padding(5.dp)
                                    .height(2.dp)
                                    .background(colors.onPrimaryContainer)
                            )
                            Text(
                                formatHoursFromSeconds(day.time),
                                style = texts.titleSmall.copy(
                                    color = colors.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))
        Text(
            "Total for måned: ${formatHoursFromSeconds(state.dayPerMonth.sumOf { it.time })}",
            style = texts.titleMedium.copy(
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}