package org.examples.time_manager.features.root.presentation.home.components

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.examples.time_manager.App
import org.examples.time_manager.R
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.HomeState
import org.examples.time_manager.features.root.data.RootScreenEvents.CreateExcelDocumentEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkStateEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.SelectDayEvent
import org.examples.time_manager.features.root.presentation.home.MonthPickerDialog
import org.examples.time_manager.features.utils.formatHoursFromSeconds
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.addIcon
import org.examples.time_manager.ui.theme.arrowDownIcon
import org.examples.time_manager.ui.theme.exportIcon

@Composable
fun HeaderWidget(
    state: HomeState,
    vm: HomeViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    var showMonthPicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    val months = stringArrayResource(R.array.months_array).toList()

    LaunchedEffect(expanded) {
        if (expanded) navigator.toCalendar()
    }

    val context = LocalContext.current
    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonthPickerViewModel", "Got a result")
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data?.getStringExtra("stringKey")?.toInt()
                Log.d("MonthPickerViewModel", "Got a result $data")
                result.data?.data?.let { uri ->
                    vm.onEvent(
                        CreateExcelDocumentEvent(context, uri, data)
                    )
                }
            }
        }

    val openSaveFilePicker = remember {
        { month: Int ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "${months.elementAt(month)}.xlsx")
                putExtra("stringKey", month)
            }
            saveFileLauncher.launch(intent)
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            onDismiss = { month: Int ->
                if (month >= 0) {
                    openSaveFilePicker(month)
                }
                showMonthPicker = false
            },
            onEvent = vm::onEvent,
            projectValues = state.projects,
        )
    }

    val density = LocalDensity.current
    val dragThresholdPx = remember(density) { with(density) { 48.dp.toPx() } }
    val selectedDate = remember(state.dayPerMonth, state.selectedDay) {
        state.dayPerMonth.firstOrNull { it.date.dayOfMonth == state.selectedDay }?.date
            ?: LocalDate.now()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(180)),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(bottomEnd = 30.dp, bottomStart = 30.dp))
                .background(colors.primary)
                .pointerInput(dragThresholdPx) {
                    detectVerticalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            if (dragTotal > dragThresholdPx) {
                                expanded = true
                            } else if (dragTotal < -dragThresholdPx) {
                                expanded = false
                            }
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
                .padding(top = App.statusBarHeight)
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        navigator.toCalendar()
                    }
                    .padding(start = 10.dp)
            ) {
                Text(
                    state.today.weekDay,
                    style = texts.headlineSmall.copy(color = colors.onPrimary),
                )
                Icon(
                    arrowDownIcon(),
                    contentDescription = null,
                    tint = colors.onPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showMonthPicker = true }) {
                    Icon(
                        exportIcon(),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = { vm.onEvent(ModifyWorkStateEvent(show = true)) }) {
                    Icon(
                        addIcon(),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            WeekHeader(
                selectedDate = selectedDate,
                monthDays = state.dayPerMonth,
                selectedDay = state.selectedDay,
                selectDay = { day -> vm.onEvent(SelectDayEvent(day)) },
                modifier = Modifier.padding(top = 8.dp)
            )

            PullHandle(
                expanded = expanded,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 2.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        expanded = !expanded
                    }
            )
        }

//        AnimatedVisibility(
//            visible = expanded,
//            enter = fadeIn(tween(150)) + slideInVertically(tween(180)) { -it / 6 },
//            exit = fadeOut(tween(120)) + slideOutVertically(tween(140)) { -it / 8 },
//        ) {
//            HomeMonthOverlay(
//                monthDays = state.dayPerMonth,
//                selectedDay = state.selectedDay,
//                onSelectDay = { day -> vm.onEvent(SelectDayEvent(day)) },
//                modifier = Modifier
//                    .padding(horizontal = 16.dp)
//                    .padding(top = 8.dp)
//            )
//        }
    }
}

@Composable
private fun PullHandle(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .width(64.dp)
            .height(18.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(if (expanded) 46.dp else 36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.onPrimary.copy(alpha = 0.52f))
            )
        }
    }
}

@Composable
private fun WeekHeader(
    selectedDate: LocalDate,
    selectedDay: Int,
    monthDays: List<DayModel>,
    selectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography
    val dayMap = remember(monthDays) { monthDays.associateBy { it.date } }
    val weekDates = remember(selectedDate) {
        val start = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
        List(7) { start.plusDays(it.toLong()) }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxWidth < 380.dp || fontScale > 1.15f
        val veryCompact = maxWidth < 340.dp || fontScale > 1.35f
        val gap = when {
            veryCompact -> 4.dp
            compact -> 5.dp
            else -> 7.dp
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            weekDates.forEach { date ->
                val model = dayMap[date]
                val isSelected = date.dayOfMonth == selectedDay && model != null
                WeekDayChip(
                    date = date,
                    dayLabel = model?.day ?: weekdayLabel(date),
                    hoursText = model?.let { formatHoursFromSeconds(it.time) } ?: "",
                    isSelected = isSelected,
                    enabled = model != null,
                    compact = compact,
                    onClick = { selectDay(date.dayOfMonth) },
                    colors = colors,
                    texts = texts,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeekDayChip(
    date: LocalDate,
    dayLabel: String,
    hoursText: String,
    isSelected: Boolean,
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    colors: androidx.compose.material3.ColorScheme,
    texts: androidx.compose.material3.Typography,
    modifier: Modifier = Modifier,
) {
    val background =
        if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh.copy(alpha = 0.34f)
    val content =
        when {
            isSelected -> colors.onPrimaryContainer
            enabled -> colors.onPrimary
            else -> colors.onPrimary.copy(alpha = 0.42f)
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(if (compact) 72.dp else 80.dp)
            .clip(RoundedCornerShape(if (isSelected) 22.dp else 18.dp))
            .background(background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 2.dp, vertical = if (compact) 6.dp else 8.dp),
    ) {
        Text(
            text = if (compact) compactWeekday(dayLabel) else dayLabel.take(3),
            color = content,
            style = texts.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        Text(
            text = date.dayOfMonth.toString(),
            style = texts.titleMedium.copy(color = content),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        if (!compact || hoursText.isNotBlank() && hoursText != "0") {
            Text(
                text = hoursText,
                color = content,
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 12.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeMonthOverlay(
    monthDays: List<DayModel>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val month = remember(monthDays) {
        monthDays.firstOrNull()?.date?.let { YearMonth.from(it) } ?: YearMonth.now()
    }
    val dayMap = remember(monthDays) { monthDays.associateBy { it.date.dayOfMonth } }
    val grid = remember(month) { buildMonthGrid(month) }
    val title = remember(month) {
        val locale = Locale.forLanguageTag("nb-NO")
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.45f)),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                color = colors.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayHeaders().forEach { label ->
                    Text(
                        text = label,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grid.chunked(7).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { day ->
                            val model = day?.let { dayMap[it] }
                            MonthGridDay(
                                day = day,
                                hoursText = model?.let { formatHoursFromSeconds(it.time) }.orEmpty(),
                                selected = day == selectedDay,
                                onClick = { if (day != null) onSelectDay(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGridDay(
    day: Int?,
    hoursText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background = if (selected) colors.primaryContainer else Color.Transparent
    val content = if (selected) colors.onPrimaryContainer else colors.onSurface

    Column(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(if (day != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (day != null) {
            Text(
                text = day.toString(),
                color = content,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
            )
            Text(
                text = if (hoursText == "0") "" else hoursText,
                color = if (selected) content else colors.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun buildMonthGrid(month: YearMonth): List<Int?> {
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val days = mutableListOf<Int?>()
    repeat(firstDayOffset) { days.add(null) }
    for (day in 1..month.lengthOfMonth()) {
        days.add(day)
    }
    while (days.size % 7 != 0) {
        days.add(null)
    }
    return days
}

private fun weekdayHeaders(): List<String> = listOf("Man", "Tir", "Ons", "Tor", "Fre", "Lør", "Søn")

private fun weekdayLabel(date: LocalDate): String =
    weekdayHeaders()[date.dayOfWeek.value - 1]

private fun compactWeekday(label: String): String =
    when (label.take(3)) {
        "Man" -> "Ma"
        "Tir" -> "Ti"
        "Ons" -> "On"
        "Tor" -> "To"
        "Fre" -> "Fr"
        "Lør" -> "Lø"
        "Søn" -> "Sø"
        else -> label.take(2)
    }
