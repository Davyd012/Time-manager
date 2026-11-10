package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

@Composable
internal fun CalendarProgressContent(
    month: LocalDate,
    selectedDate: LocalDate,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    weekdays: List<String>,
    weekDays: List<LocalDate>,
    weekSeconds: Map<LocalDate, Int>,
    expansionProgress: Float,
    expanded: Boolean,
    expandedContentEnabled: Boolean,
    animationsEnabled: Boolean,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onMonthView: (LocalDate) -> Unit,
    onToggleExpanded: (() -> Unit)?,
    onShareMonth: ((String, String) -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (expansionProgress < 1f) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .alpha(1f - expansionProgress)
                    .then(if (expansionProgress > 0f) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                CalendarWeekStrip(
                    weekDays = weekDays,
                    weekdays = weekdays,
                    secondsByDay = weekSeconds,
                    selectedDate = selectedDate,
                    expansionProgress = expansionProgress,
                    expanded = expanded,
                    onSelectDate = onSelectDate,
                    onToggleExpanded = onToggleExpanded,
                )
            }
        }
        if (expansionProgress > 0f) {
            CalendarMonthTransitionContent(
                month = month,
                selectedDate = selectedDate,
                works = works,
                projects = projects,
                selectedProjectIds = selectedProjectIds,
                weekdays = weekdays,
                expansionProgress = expansionProgress,
                interactive = expandedContentEnabled,
                animationsEnabled = animationsEnabled,
                onMonthChange = onMonthChange,
                onSelectDate = onSelectDate,
                onMonthView = onMonthView,
                onShareMonth = onShareMonth,
            )
        }
    }
}
