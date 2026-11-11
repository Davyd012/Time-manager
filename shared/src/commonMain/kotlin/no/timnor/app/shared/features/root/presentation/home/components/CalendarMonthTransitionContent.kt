package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

@Composable
internal fun CalendarMonthTransitionContent(
    month: LocalDate,
    selectedGridDate: LocalDate?,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    weekdays: List<String>,
    expansionProgress: Float,
    interactive: Boolean,
    animationsEnabled: Boolean,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onMonthView: (LocalDate) -> Unit,
    onShareMonth: ((String, String) -> Unit)?,
) {
    AnimatedContent(
        targetState = month,
        modifier = Modifier.fillMaxWidth()
            .alpha(expansionProgress)
            .then(if (interactive) Modifier else Modifier.clearAndSetSemantics {}),
        transitionSpec = {
            monthTransitionSpec(
                forward = targetState > initialState,
                enabled = animationsEnabled && targetState != initialState,
            )
        },
        label = "calendar-month-transition",
    ) { displayedMonth ->
        CalendarMonthContent(
            month = displayedMonth,
            currentMonth = month,
            selectedDate = selectedGridDate,
            works = works,
            projects = projects,
            selectedProjectIds = selectedProjectIds,
            interactive = interactive,
            weekdays = weekdays,
            onMonthChange = onMonthChange,
            onSelectDate = onSelectDate,
            onOpenMonthView = onMonthView,
            onShareMonth = onShareMonth,
        )
    }
}
