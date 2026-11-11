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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.formatCalendarHours
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.calendar.presentation.components.MonthShareSummaryKind
import no.timnor.app.shared.features.calendar.presentation.components.formatMonthShareHours
import no.timnor.app.shared.features.calendar.presentation.components.monthShareSummaryKind
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
import no.timnor.app.shared.generated.resources.calendar_month_total
import no.timnor.app.shared.generated.resources.calendar_next_month
import no.timnor.app.shared.generated.resources.calendar_previous_month
import no.timnor.app.shared.generated.resources.share
import no.timnor.app.shared.generated.resources.share_chooser_title
import no.timnor.app.shared.generated.resources.summary_no_hours
import no.timnor.app.shared.generated.resources.summary_quiet_month
import no.timnor.app.shared.generated.resources.summary_normal_month
import no.timnor.app.shared.generated.resources.summary_active_month
import no.timnor.app.shared.generated.resources.hour_singular
import no.timnor.app.shared.generated.resources.hour_plural
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarMonthContent(
    month: LocalDate,
    currentMonth: LocalDate,
    selectedDate: LocalDate?,
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
    val monthSeconds = remember(visibleMonthWorks) { visibleMonthWorks.sumOf { it.time.toLong() } }
    val monthTotal = remember(monthSeconds) { formatCalendarHours(monthSeconds) }
    val monthTotalMinutes = (monthSeconds + 30) / 60
    val monthTitle = "${monthName(month.month).replaceFirstChar { it.titlecase() }} ${month.year}"
    val monthLabel = "${monthName(month.month)} ${month.year}"
    val monthSummary = when (monthShareSummaryKind(monthSeconds)) {
        MonthShareSummaryKind.NoHours -> stringResource(Res.string.summary_no_hours, monthLabel)
        MonthShareSummaryKind.Quiet -> stringResource(
            Res.string.summary_quiet_month,
            monthLabel,
            formatMonthShareHours(monthSeconds),
            stringResource(if (monthSeconds == 3600L) Res.string.hour_singular else Res.string.hour_plural),
        )
        MonthShareSummaryKind.Normal -> stringResource(
            Res.string.summary_normal_month,
            formatMonthShareHours(monthSeconds),
            stringResource(if (monthSeconds == 3600L) Res.string.hour_singular else Res.string.hour_plural),
            monthLabel,
        )
        MonthShareSummaryKind.Active -> stringResource(
            Res.string.summary_active_month,
            formatMonthShareHours(monthSeconds),
            stringResource(if (monthSeconds == 3600L) Res.string.hour_singular else Res.string.hour_plural),
            monthLabel,
        )
    }
    val shareSubject = stringResource(Res.string.share_chooser_title)
    val previousMonthLabel = stringResource(Res.string.calendar_previous_month)
    val nextMonthLabel = stringResource(Res.string.calendar_next_month)
    val shareMonthLabel = stringResource(Res.string.calendar_month_share)
    val swipeThreshold = with(LocalDensity.current) { (MaterialTheme.timnorSpacing.extraLarge * 2).toPx() }
    fun requestMonthChange(target: LocalDate) {
        if (interactive && target != currentMonth) {
            onMonthChange(target)
        }
    }

    Column(
        modifier = Modifier.testTag("calendar-month-content").pointerInput(currentMonth, interactive, swipeThreshold) {
            if (interactive) {
                var horizontalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        horizontalDrag += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (horizontalDrag <= -swipeThreshold) {
                            requestMonthChange(currentMonth.plus(1, DateTimeUnit.MONTH))
                        } else if (horizontalDrag >= swipeThreshold) {
                            requestMonthChange(currentMonth.minus(1, DateTimeUnit.MONTH))
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f },
                )
            }
        },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small, Alignment.CenterHorizontally),
        ) {
            OutlinedIconButton(
                onClick = { requestMonthChange(currentMonth.minus(1, DateTimeUnit.MONTH)) },
                modifier = Modifier.semantics { contentDescription = previousMonthLabel },
                enabled = interactive,
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
            }
            Surface(
                onClick = { onOpenMonthView(LocalDate(month.year, month.month, 1)) },
                modifier = Modifier.weight(1f, fill = false),
                enabled = interactive,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = monthTitle,
                    modifier = Modifier.padding(MaterialTheme.timnorSpacing.medium),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedIconButton(
                onClick = { requestMonthChange(currentMonth.plus(1, DateTimeUnit.MONTH)) },
                modifier = Modifier.semantics { contentDescription = nextMonthLabel },
                enabled = interactive,
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(painterResource(Res.drawable.arrow_right), contentDescription = null)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.timnorSpacing.extraSmall)) {
                CalendarMonthGrid(
                    month = month,
                    layout = layout,
                    secondsByDay = byDay,
                    selectedDate = selectedDate,
                    weekdays = weekdays,
                    interactive = interactive,
                    onSelectDate = onSelectDate,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.timnorSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(
                            stringResource(Res.string.calendar_month_total, monthTotalMinutes / 60, monthTotalMinutes % 60),
                            modifier = Modifier.padding(MaterialTheme.timnorSpacing.medium),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    onShareMonth?.let { share ->
                        IconButton(
                            onClick = { share(shareSubject, monthSummary) },
                            modifier = Modifier.semantics { contentDescription = shareMonthLabel },
                            enabled = interactive,
                        ) {
                            Icon(painterResource(Res.drawable.share), contentDescription = null)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
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
