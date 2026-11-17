package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.ScrollState
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

/** Keeps one month/date grid composed while measured date cells morph. */
@Composable
internal fun CalendarMorphContent(
    month: LocalDate,
    selectedDate: LocalDate,
    selectedGridDate: LocalDate?,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    weekdays: List<String>,
    expansionProgress: Float,
    scrollState: ScrollState,
    interactive: Boolean,
    onMonthChange: (LocalDate) -> Unit,
    onSelectWeekDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onMonthView: (LocalDate) -> Unit,
    onShareMonth: ((String, String) -> Unit)?,
) {
    CalendarMonthContent(
        month = month,
        currentMonth = month,
        selectedDate = selectedGridDate,
        homeSelectedDate = selectedDate,
        works = works,
        projects = projects,
        selectedProjectIds = selectedProjectIds,
        interactive = interactive,
        weekdays = weekdays,
        expansionProgress = expansionProgress,
        scrollState = scrollState,
        onSelectWeekDate = onSelectWeekDate,
        onMonthChange = onMonthChange,
        onSelectDate = onSelectDate,
        onOpenMonthView = onMonthView,
        onShareMonth = onShareMonth,
    )
}
