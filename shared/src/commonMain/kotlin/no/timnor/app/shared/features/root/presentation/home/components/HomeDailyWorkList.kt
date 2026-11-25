package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.material3.MaterialTheme
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewEmptyState
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewTimeline
import no.timnor.app.shared.features.month_view.presentation.components.monthViewWorksForDate
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled

@Composable
internal fun HomeDailyWorkList(
    selectedDate: LocalDate,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedWorkIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    expansionProgress: Float,
    modifier: Modifier = Modifier,
    isDraggingCalendar: Boolean,
    onEditWork: (StoredWork) -> Unit,
    onAddWork: () -> Unit,
) {
    val selectedWorks = remember(works, selectedDate) { monthViewWorksForDate(works, selectedDate) }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    AnimatedContent(
        targetState = selectedWorks.isEmpty(),
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (shouldBlockHomeWorkListInput(expansionProgress) && !isDraggingCalendar) {
                    Modifier
                        .clearAndSetSemantics { hideFromAccessibility() }
                } else Modifier,
            ),
        label = "homeWorkListEmptyTransition",
        transitionSpec = {
            if (!animationsEnabled) {
                (EnterTransition.None togetherWith ExitTransition.None).using(SizeTransform(clip = false))
            } else {
                val initialOffset: (Int) -> Int = { height -> if (targetState) height / 12 else -height / 12 }
                (fadeIn(tween(180)) + slideInVertically(tween(220), initialOffsetY = initialOffset))
                    .togetherWith(ExitTransition.None)
                    .using(SizeTransform(clip = false))
            }
        },
    ) { empty ->
        if (empty) {
            MonthViewEmptyState(
                onAddWork = onAddWork,
                modifier = Modifier.fillMaxSize()
                    .testTag("home-empty-work-drag-surface"),
                loopAnimation = true,
            )
        } else {
            MonthViewTimeline(
                works = selectedWorks,
                projects = projects,
                modifier = Modifier.fillMaxSize(),
                shape = homeDailyTimelineShape(MaterialTheme.shapes.large),
                showDuration = true,
                showProjectMarker = true,
                showEntryDetails = true,
                linkedPlannorProjectNames = linkedPlannorProjectNames,
                sharedWorkIndicators = sharedWorkIndicators,
                onSelectWork = onEditWork,
            )
        }
    }
}

internal fun shouldBlockHomeWorkListInput(expansionProgress: Float): Boolean =
    expansionProgress > 0.8f

internal fun homeDailyTimelineShape(shape: CornerBasedShape) =
    shape.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
