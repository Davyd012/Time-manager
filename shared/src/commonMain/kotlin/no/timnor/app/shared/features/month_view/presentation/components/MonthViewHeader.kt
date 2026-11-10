package no.timnor.app.shared.features.month_view.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.back_to_home
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.domain.calendar.formatCalendarHours
import no.timnor.app.shared.generated.resources.calendar_weekday_friday
import no.timnor.app.shared.generated.resources.calendar_weekday_monday
import no.timnor.app.shared.generated.resources.calendar_weekday_saturday
import no.timnor.app.shared.generated.resources.calendar_weekday_sunday
import no.timnor.app.shared.generated.resources.calendar_weekday_thursday
import no.timnor.app.shared.generated.resources.calendar_weekday_tuesday
import no.timnor.app.shared.generated.resources.calendar_weekday_wednesday
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MonthViewHeader(
    monthTitle: String,
    daysInMonth: Int,
    secondsByDay: Map<Int, Int>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val weekdays = listOf(
        stringResource(Res.string.calendar_weekday_monday),
        stringResource(Res.string.calendar_weekday_tuesday),
        stringResource(Res.string.calendar_weekday_wednesday),
        stringResource(Res.string.calendar_weekday_thursday),
        stringResource(Res.string.calendar_weekday_friday),
        stringResource(Res.string.calendar_weekday_saturday),
        stringResource(Res.string.calendar_weekday_sunday),
    )
    val dayListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedDate.day - 4).coerceAtLeast(0),
    )
    val backLabel = stringResource(Res.string.back_to_home)
    LaunchedEffect(selectedDate.year, selectedDate.month) {
        dayListState.scrollToItem((selectedDate.day - 4).coerceAtLeast(0))
    }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backLabel }) {
                androidx.compose.material3.Icon(
                    painter = org.jetbrains.compose.resources.painterResource(Res.drawable.arrow_back),
                    contentDescription = null,
                )
            }
            Text(monthTitle, style = MaterialTheme.typography.titleLarge)
        }
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
        ) {
            LazyRow(
                state = dayListState,
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
            ) {
                items(daysInMonth, key = { day -> day }) { day ->
                    val date = LocalDate(selectedDate.year, selectedDate.month.ordinal + 1, day)
                    Surface(
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.semantics { selected = date == selectedDate },
                        color = if (date == selectedDate) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.primary,
                        contentColor = if (date == selectedDate) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.width(MaterialTheme.timnorSpacing.extraLarge * 2)
                                .heightIn(min = MaterialTheme.timnorSpacing.extraLarge * 2)
                                .padding(MaterialTheme.timnorSpacing.small),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                        ) {
                            Text(weekdays[date.dayOfWeek.ordinal].take(3), style = MaterialTheme.typography.labelMedium)
                            Text(date.day.toString(), style = MaterialTheme.typography.titleMedium)
                            val seconds = secondsByDay[day] ?: 0
                            if (seconds > 0) {
                                Text(
                                    "${formatCalendarHours(seconds.toLong())}h",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            } else {
                                Spacer(Modifier.size(MaterialTheme.timnorSpacing.small))
                            }
                        }
                    }
                }
            }
        }
    }
}
