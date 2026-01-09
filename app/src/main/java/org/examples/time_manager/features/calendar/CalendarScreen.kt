package org.examples.time_manager.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.examples.time_manager.App
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.features.calendar.data.CalendarScreenEvents
import org.examples.time_manager.features.calendar.presentation.components.ProjectsList
import org.examples.time_manager.features.calendar.utils.formatHoursFromSeconds
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.visibilityIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar

@Composable
fun CalendarScreen(vm: CalendarViewModel, navigator: Navigator) {
    val state by vm.state.collectAsState()

    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    val months = remember {
        listOf(
            "Januar", "Februar", "Mars", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Desember"
        )
    }

    val firstDay = state.dayPerMonth.firstOrNull()?.date
    val firstDayOrd = firstDay?.dayOfWeek?.ordinal ?: 0
    var month = firstDay?.month?.ordinal ?: 0

    val days =
        List(firstDayOrd) { null } + (1..(state.dayPerMonth.size.takeIf { it > 0 }
            ?: 2)).toList()

    Column(
        modifier = Modifier
            .background(colors.primary)
            .padding(top = App.statusBarHeight)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Text(
                state.currentDate.year.toString() + " / " + months.elementAt(normalizeIndex(month)),
                style = texts.headlineSmall.copy(
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .padding(5.dp)
                    .clickable {
                        navigator.close()
                    }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .padding(5.dp)
                    .clickable {
                        month -= 1
                        vm.onEvent(CalendarScreenEvents.UpdateMonth(-1))
                    }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        val instance = Calendar.getInstance()
                        val date = instance.apply {
                            add(Calendar.MONTH, state.monthDifference)
                        }.timeInMillis.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC) }
                        val stringDate = LocalDate.of(date.year, date.month, 1).toString()
                        navigator.toMonthView(date = stringDate, day = 1)
                    }
                    .padding(10.dp)
            ) {
                Text(
                    "Se måned",
                    style = texts.headlineSmall.copy(
                        color = colors.onPrimary,
                    ),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(visibilityIcon(), contentDescription = null, tint = colors.onPrimary)
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .padding(5.dp)
                    .clickable {
                        month += 1
                        vm.onEvent(CalendarScreenEvents.UpdateMonth(1))
                    }
            )
        }
        Spacer(modifier = Modifier.height(25.dp))

        ProjectsList(
            projectsState = state.projects,
            selectedProjects = state.selectedProjects,
            selectProject = { project: Project ->
                vm.onEvent(
                    CalendarScreenEvents.UpdateSelectedProjects(project)
                )
            },
        )
        Spacer(modifier = Modifier.height(25.dp))

        DaysList(
            days,
            monthDays = state.dayPerMonth,
            firstDayOrd = firstDayOrd,
            selectDay = { index: Int ->
                val instance = Calendar.getInstance()
                val date = instance.apply {
                    add(Calendar.MONTH, state.monthDifference)
                }.timeInMillis.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC) }
                val stringDate = LocalDate.of(date.year, date.month, 1).toString()
                navigator.toMonthView(date = stringDate, day = index)
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Total for måned: ${formatHoursFromSeconds(state.dayPerMonth.sumOf { it.time })}",
            style = texts.titleMedium.copy(
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DaysList(
    days: List<Int?>,
    monthDays: List<DayModel>,
    firstDayOrd: Int,
    selectDay: (index: Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.padding(16.dp)
    ) {
        items(days.size) { value ->
            if (days.elementAt(value) == null) return@items

            if (monthDays.isNotEmpty()) {
                val day = monthDays.elementAt(value - firstDayOrd)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
//                            .padding(4.dp)
//                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .wrapContentHeight(unbounded = true)
//                            .background(colors.primaryContainer, shape = RoundedCornerShape(4.dp))
                        .padding(10.dp)
                        .clickable { selectDay(day.date.dayOfMonth) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day.date.dayOfMonth.toString(),
                            style = texts.titleMedium.copy(color = colors.onPrimary)
                        )
//                            Box(
//                                modifier = Modifier
//                                    .width(30.dp)
//                                    .padding(5.dp)
//                                    .height(2.dp)
//                                    .background(colors.onPrimaryContainer)
//                            )
                        Text(
                            formatHoursFromSeconds(day.time),
                            style = texts.titleSmall.copy(
                                color = colors.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun normalizeIndex(month: Int): Int {
    val value = if (month < 0) month * -1 else month
    val result = value % 12
    return if (month < 0) 12 - result else result
}