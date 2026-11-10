package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.MonthGridLayout
import no.timnor.app.shared.domain.calendar.formatCalendarHours
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.generated.resources.arrow_right
import no.timnor.app.shared.generated.resources.calendar_month_april
import no.timnor.app.shared.generated.resources.calendar_month_august
import no.timnor.app.shared.generated.resources.calendar_month_december
import no.timnor.app.shared.generated.resources.calendar_month_february
import no.timnor.app.shared.generated.resources.calendar_month_january
import no.timnor.app.shared.generated.resources.calendar_month_july
import no.timnor.app.shared.generated.resources.calendar_month_june
import no.timnor.app.shared.generated.resources.calendar_month_march
import no.timnor.app.shared.generated.resources.calendar_month_may
import no.timnor.app.shared.generated.resources.calendar_month_november
import no.timnor.app.shared.generated.resources.calendar_month_october
import no.timnor.app.shared.generated.resources.calendar_month_september
import no.timnor.app.shared.generated.resources.calendar_month_share
import no.timnor.app.shared.generated.resources.calendar_month_share_summary
import no.timnor.app.shared.generated.resources.calendar_month_total
import no.timnor.app.shared.generated.resources.calendar_next_month
import no.timnor.app.shared.generated.resources.calendar_previous_month
import no.timnor.app.shared.generated.resources.share
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarMonthContent(
    month: LocalDate,
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    interactive: Boolean,
    weekdays: List<String>,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenMonthView: (LocalDate) -> Unit,
    onShareMonth: ((String, String) -> Unit)?,
) {
    val monthNumber = month.month.ordinal + 1
    val layout = remember(month.year, monthNumber) { monthGridLayout(month.year, monthNumber) }
    val visibleMonthWorks = remember(works, projects, selectedProjectIds, month) {
        visibleWorks(works, projects, selectedProjectIds).filter { work ->
            val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
            date.year == month.year && date.month == month.month
        }
    }
    val byDay = remember(month, visibleMonthWorks) {
        workSecondsByDay(
            entries = visibleMonthWorks,
            year = month.year,
            month = monthNumber,
            dateOf = { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date },
            secondsOf = StoredWork::time,
        )
    }
    val monthTotal = remember(visibleMonthWorks) {
        formatCalendarHours(visibleMonthWorks.sumOf { it.time.toLong() })
    }
    val monthTitle = "${monthName(month.month)} ${month.year}"
    val monthSummary = stringResource(Res.string.calendar_month_share_summary, monthTitle, monthTotal)
    val previousMonthLabel = stringResource(Res.string.calendar_previous_month)
    val nextMonthLabel = stringResource(Res.string.calendar_next_month)
    val shareMonthLabel = stringResource(Res.string.calendar_month_share)
    fun requestMonthChange(target: LocalDate) {
        if (interactive && target != currentMonth) {
            onMonthChange(target)
        }
    }

    Column(
        modifier = Modifier.pointerInput(currentMonth, interactive) {
            if (interactive) {
                var horizontalDrag = 0f
                var changedMonth = false
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        horizontalDrag += amount
                        if (!changedMonth && horizontalDrag < -size.width * 0.2f) {
                            changedMonth = true
                            requestMonthChange(currentMonth.plus(1, DateTimeUnit.MONTH))
                        } else if (!changedMonth && horizontalDrag > size.width * 0.2f) {
                            changedMonth = true
                            requestMonthChange(currentMonth.minus(1, DateTimeUnit.MONTH))
                        }
                        change.consume()
                    },
                    onDragEnd = { horizontalDrag = 0f },
                    onDragCancel = { horizontalDrag = 0f },
                )
            }
        },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick = { requestMonthChange(currentMonth.minus(1, DateTimeUnit.MONTH)) },
                modifier = Modifier.semantics { contentDescription = previousMonthLabel },
                enabled = interactive,
            ) {
                Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
            }
            Text(
                text = "${monthName(month.month)} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                onClick = { requestMonthChange(currentMonth.plus(1, DateTimeUnit.MONTH)) },
                modifier = Modifier.semantics { contentDescription = nextMonthLabel },
                enabled = interactive,
            ) {
                Icon(painterResource(Res.drawable.arrow_right), contentDescription = null)
            }
        }
        CalendarMonthGrid(
            month = month,
            layout = layout,
            secondsByDay = byDay,
            selectedDate = selectedDate,
            weekdays = weekdays,
            interactive = interactive,
            onSelectDate = onOpenMonthView,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.timnorSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.calendar_month_total, monthTotal), style = MaterialTheme.typography.titleMedium)
                onShareMonth?.let { share ->
                    IconButton(
                        onClick = { share(monthTitle, monthSummary) },
                        modifier = Modifier.semantics {
                            contentDescription = shareMonthLabel
                        },
                        enabled = interactive,
                    ) {
                        Icon(painterResource(Res.drawable.share), contentDescription = null)
                    }
                }
            }
        }
    }
}

internal fun monthTransitionSpec(forward: Boolean, enabled: Boolean): ContentTransform {
    if (!enabled) return ContentTransform(EnterTransition.None, ExitTransition.None)

    val duration = 300
    val enterOffset: (Int) -> Int = { width -> if (forward) width else -width }
    val exitOffset: (Int) -> Int = { width -> if (forward) -width else width }
    return (
        fadeIn(tween(duration)) +
            slideInHorizontally(tween(duration), initialOffsetX = enterOffset)
        ) togetherWith (
        fadeOut(tween(duration)) +
            slideOutHorizontally(tween(duration), targetOffsetX = exitOffset)
        )
}

internal fun visibleWorks(
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
): List<StoredWork> {
    if (selectedProjectIds == null) return works
    val validProjectIds = projects.mapTo(mutableSetOf(), StoredProject::id)
    return works.filter { it.project in selectedProjectIds && it.project in validProjectIds }
}

@Composable
private fun monthName(month: Month): String = stringResource(
    when (month) {
        Month.JANUARY -> Res.string.calendar_month_january
        Month.FEBRUARY -> Res.string.calendar_month_february
        Month.MARCH -> Res.string.calendar_month_march
        Month.APRIL -> Res.string.calendar_month_april
        Month.MAY -> Res.string.calendar_month_may
        Month.JUNE -> Res.string.calendar_month_june
        Month.JULY -> Res.string.calendar_month_july
        Month.AUGUST -> Res.string.calendar_month_august
        Month.SEPTEMBER -> Res.string.calendar_month_september
        Month.OCTOBER -> Res.string.calendar_month_october
        Month.NOVEMBER -> Res.string.calendar_month_november
        Month.DECEMBER -> Res.string.calendar_month_december
    },
)
