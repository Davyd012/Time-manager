package no.timnor.app.shared.features.month_view.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.back_to_home
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.domain.calendar.formatDayHours
import no.timnor.app.shared.generated.resources.month_view_weekday_friday
import no.timnor.app.shared.generated.resources.month_view_weekday_monday
import no.timnor.app.shared.generated.resources.month_view_weekday_saturday
import no.timnor.app.shared.generated.resources.month_view_weekday_sunday
import no.timnor.app.shared.generated.resources.month_view_weekday_thursday
import no.timnor.app.shared.generated.resources.month_view_weekday_tuesday
import no.timnor.app.shared.generated.resources.month_view_weekday_wednesday
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MonthViewHeader(
    daysInMonth: Int,
    secondsByDay: Map<Int, Int>,
    selectedDate: LocalDate,
    scrollToSelectedDateKey: Int,
    onSelectDate: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val weekdays = listOf(
        stringResource(Res.string.month_view_weekday_monday),
        stringResource(Res.string.month_view_weekday_tuesday),
        stringResource(Res.string.month_view_weekday_wednesday),
        stringResource(Res.string.month_view_weekday_thursday),
        stringResource(Res.string.month_view_weekday_friday),
        stringResource(Res.string.month_view_weekday_saturday),
        stringResource(Res.string.month_view_weekday_sunday),
    )
    val dayListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedDate.day - 4).coerceAtLeast(0),
    )
    val backLabel = stringResource(Res.string.back_to_home)
    val animationsEnabled = rememberSystemAnimationsEnabled()
    LaunchedEffect(selectedDate.year, selectedDate.month, scrollToSelectedDateKey) {
        dayListState.scrollToItem((selectedDate.day - 4).coerceAtLeast(0))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.timnorSpacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
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
                Spacer(Modifier.weight(1f))
            }
            LazyRow(
                state = dayListState,
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
            ) {
                items(daysInMonth, key = { index -> index + 1 }) { index ->
                    val day = index + 1
                    val date = LocalDate(selectedDate.year, selectedDate.month.ordinal + 1, day)
                    val selected = date == selectedDate
                    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                    val pressed = interactionSource.collectIsPressedAsState().value
                    val scale by animateFloatAsState(
                        targetValue = monthViewDayCellScale(selected, pressed),
                        animationSpec = if (animationsEnabled) {
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        } else snap(),
                        label = "monthViewDayCellScale",
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.primary,
                        animationSpec = if (animationsEnabled) tween(160) else snap(),
                        label = "monthViewDayCellContainerColor",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onPrimary,
                        animationSpec = if (animationsEnabled) tween(160) else snap(),
                        label = "monthViewDayCellContentColor",
                    )
                    Surface(
                        onClick = { onSelectDate(date) },
                        modifier = Modifier
                            .testTag("month-day-$day")
                            .scale(scale)
                            .semantics { this.selected = selected },
                        interactionSource = interactionSource,
                        color = containerColor,
                        contentColor = contentColor,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.widthIn(
                                min = MaterialTheme.timnorSpacing.extraLarge + MaterialTheme.timnorSpacing.large,
                            )
                                .heightIn(min = MaterialTheme.timnorSpacing.extraLarge * 2)
                                .padding(
                                    horizontal = MaterialTheme.timnorSpacing.small,
                                    vertical = MaterialTheme.timnorSpacing.extraSmall,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
                        ) {
                            Text(
                                weekdays[date.dayOfWeek.ordinal],
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                date.day.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val seconds = secondsByDay[day] ?: 0
                            Text(
                                formatDayHours(seconds.toLong()),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun monthViewDayCellScale(selected: Boolean, pressed: Boolean): Float = when {
    pressed -> 0.96f
    selected -> 1.04f
    else -> 1f
}
