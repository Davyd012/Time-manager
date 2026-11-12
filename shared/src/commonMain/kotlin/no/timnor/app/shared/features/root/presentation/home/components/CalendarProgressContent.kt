package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
internal fun CalendarProgressContent(
    month: LocalDate,
    selectedDate: LocalDate,
    selectedGridDate: LocalDate?,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    weekdays: List<String>,
    expansionProgress: Float,
    expanded: Boolean,
    expandedContentEnabled: Boolean,
    onMonthChange: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectGridDate: (LocalDate) -> Unit,
    onMonthView: (LocalDate) -> Unit,
    onToggleExpanded: (() -> Unit)?,
    onShareMonth: ((String, String) -> Unit)?,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
        MaterialTheme.timnorSpacing.small * expansionProgress.coerceIn(0f, 1f),
    )) {
        CalendarMorphContent(
            month = month,
            selectedDate = selectedDate,
            selectedGridDate = selectedGridDate,
            works = works,
            projects = projects,
            selectedProjectIds = selectedProjectIds,
            weekdays = weekdays,
            expansionProgress = expansionProgress,
            interactive = expandedContentEnabled,
            onMonthChange = onMonthChange,
            onSelectWeekDate = onSelectDate,
            onSelectDate = onSelectGridDate,
            onMonthView = onMonthView,
            onShareMonth = onShareMonth,
        )
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(MaterialTheme.timnorSpacing.medium * 3 * fontScale * (1f - expansionProgress.coerceIn(0f, 1f)))
                .clipToBounds()
                .alpha(1f - expansionProgress.coerceIn(0f, 1f))
                .then(if (isHomeWeekRowInteractive(expansionProgress)) Modifier else Modifier.clearAndSetSemantics {}),
            contentAlignment = Alignment.Center,
        ) {
            HomeCalendarToggle(
                expanded = expanded,
                onToggle = { onToggleExpanded?.invoke() },
                enabled = isHomeWeekRowInteractive(expansionProgress),
            )
        }
    }
}
