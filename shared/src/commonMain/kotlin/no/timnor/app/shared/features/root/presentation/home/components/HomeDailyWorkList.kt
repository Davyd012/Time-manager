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
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.material3.MaterialTheme
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewEmptyState
import no.timnor.app.shared.features.month_view.presentation.components.MonthViewTimeline
import no.timnor.app.shared.features.month_view.presentation.components.monthViewWorksForDate
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
internal fun HomeDailyWorkList(
    selectedDate: LocalDate,
    works: List<StoredWork>,
    projects: List<StoredProject>,
    expansionProgress: Float,
    modifier: Modifier = Modifier,
    expansionDragState: DraggableState,
    onExpansionDragStarted: () -> Unit,
    onExpansionDragStopped: (Float) -> Unit,
    onExpansionProgressChange: (Float) -> Unit,
    expansionDragDistancePx: Float,
    isDraggingCalendar: Boolean,
    onEditWork: (StoredWork) -> Unit,
    onAddWork: () -> Unit,
) {
    val selectedWorks = remember(works, selectedDate) { monthViewWorksForDate(works, selectedDate) }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val expansionTranslationPx = with(LocalDensity.current) { MaterialTheme.timnorSpacing.medium.toPx() }
    var handingOffScroll by remember { mutableStateOf(false) }
    var scrollDragProgress by remember { mutableFloatStateOf(expansionProgress) }
    val latestExpansionProgress = androidx.compose.runtime.rememberUpdatedState(expansionProgress)
    val latestOnDragStarted = androidx.compose.runtime.rememberUpdatedState(onExpansionDragStarted)
    val latestOnDragStopped = androidx.compose.runtime.rememberUpdatedState(onExpansionDragStopped)
    val latestOnProgressChange = androidx.compose.runtime.rememberUpdatedState(onExpansionProgressChange)
    val latestDragDistance = androidx.compose.runtime.rememberUpdatedState(expansionDragDistancePx)
    val scrollHandoff = remember(expansionDragState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (latestExpansionProgress.value > 0.8f) return Offset.Zero
                if (kotlin.math.abs(available.y) < 1f) return Offset.Zero
                if (!handingOffScroll) {
                    latestOnDragStarted.value()
                    scrollDragProgress = latestExpansionProgress.value
                    handingOffScroll = true
                }
                scrollDragProgress = (scrollDragProgress - available.y / latestDragDistance.value.coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
                latestOnProgressChange.value(scrollDragProgress)
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (handingOffScroll) {
                    latestOnDragStopped.value(available.y)
                    handingOffScroll = false
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    AnimatedContent(
        targetState = selectedWorks.isEmpty(),
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer {
                alpha = 1f - (expansionProgress / 0.25f).coerceIn(0f, 1f)
                translationY = expansionProgress * expansionTranslationPx
            }
            .then(
                if (shouldBlockHomeWorkListInput(expansionProgress) && !isDraggingCalendar) {
                    Modifier
                        .clearAndSetSemantics { hideFromAccessibility() }
                        .pointerInput(shouldBlockHomeWorkListInput(expansionProgress)) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
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
                    .testTag("home-empty-work-drag-surface")
                    .calendarExpansionDrag(
                        state = expansionDragState,
                        onStarted = onExpansionDragStarted,
                        onStopped = onExpansionDragStopped,
                    ),
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
                nestedScrollConnection = scrollHandoff,
                onSelectWork = onEditWork,
            )
        }
    }
}

internal fun shouldBlockHomeWorkListInput(expansionProgress: Float): Boolean =
    expansionProgress > 0.8f

internal fun homeDailyTimelineShape(shape: CornerBasedShape) =
    shape.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
