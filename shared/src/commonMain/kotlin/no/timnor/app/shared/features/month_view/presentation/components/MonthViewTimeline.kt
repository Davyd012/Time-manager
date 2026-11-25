package no.timnor.app.shared.features.month_view.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.features.shared_hours.presentation.SharedWorkSyncLabel
import no.timnor.app.shared.domain.project.workPayOre
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.root.presentation.home.components.workDurationLabel
import no.timnor.app.shared.features.root.presentation.home.components.workPayLabel
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.unknown_project
import no.timnor.app.shared.generated.resources.plannor_linked_project_label
import no.timnor.app.shared.generated.resources.plannor_project_marker_description
import no.timnor.app.shared.generated.resources.plannor_link_24
import no.timnor.app.shared.generated.resources.work_entry_time_range
import no.timnor.app.shared.platform.locale.localizedDecimalSeparator
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MonthViewTimeline(
    works: List<StoredWork>,
    projects: List<StoredProject>,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    showDuration: Boolean = true,
    showProjectMarker: Boolean = false,
    showEntryDetails: Boolean = false,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedWorkIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    nestedScrollConnection: NestedScrollConnection? = null,
    onSelectWork: (StoredWork) -> Unit,
) {
    val unknownProject = stringResource(Res.string.unknown_project)
    val groupedWorks = remember(works) { monthViewWorkGroups(works).toList() }
    val projectsById = remember(projects) { projects.associateBy(StoredProject::id) }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = shape ?: MaterialTheme.shapes.large,
    ) {
        LazyColumn(
            modifier = (if (nestedScrollConnection == null) Modifier else Modifier.nestedScroll(nestedScrollConnection))
                .fillMaxWidth().padding(
                if (showProjectMarker) {
                    MaterialTheme.timnorSpacing.small + MaterialTheme.timnorSpacing.extraSmall / 2
                } else MaterialTheme.timnorSpacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (showProjectMarker) {
                    MaterialTheme.timnorSpacing.small - MaterialTheme.timnorSpacing.extraSmall / 2
                } else MaterialTheme.timnorSpacing.small,
            ),
        ) {
            groupedWorks.forEach { (hour, entries) ->
                item(key = "hour-$hour") {
                    MonthViewTimelineGroup(
                        hour = hour,
                        works = entries,
                        projectName = { work -> projectsById[work.project]?.name ?: unknownProject },
                        payForWork = { work -> workPayOre(work, projectsById[work.project]) },
                        linkedPlannorProjectName = { work -> linkedPlannorProjectNames[work.project] },
                        showDuration = showDuration,
                        showProjectMarker = showProjectMarker,
                        showEntryDetails = showEntryDetails,
                        sharedWorkIndicators = sharedWorkIndicators,
                        onSelectWork = onSelectWork,
                    )
                }
            }
        }
    }
}

internal fun monthViewWorkGroups(works: List<StoredWork>): Map<Int, List<StoredWork>> =
    works.sortedBy(StoredWork::dateEpochSeconds)
        .groupBy { work -> utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).hour }

internal fun monthViewWorksForDate(works: List<StoredWork>, date: LocalDate): List<StoredWork> =
    works.asSequence()
        .filter { work -> utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date == date }
        .sortedBy(StoredWork::dateEpochSeconds)
        .toList()

@Composable
private fun MonthViewTimelineGroup(
    hour: Int,
    works: List<StoredWork>,
    projectName: (StoredWork) -> String,
    payForWork: (StoredWork) -> Long?,
    linkedPlannorProjectName: (StoredWork) -> String?,
    showDuration: Boolean,
    showProjectMarker: Boolean,
    showEntryDetails: Boolean,
    sharedWorkIndicators: Map<Int, SharedWorkSyncIndicator>,
    onSelectWork: (StoredWork) -> Unit,
) {
    val homeRowSpacing = MaterialTheme.timnorSpacing.extraSmall + MaterialTheme.timnorSpacing.extraSmall / 4
    val groupVerticalPadding = if (showProjectMarker) 0.dp else MaterialTheme.timnorSpacing.small
    val workCardMinHeight = if (showProjectMarker) {
        MaterialTheme.timnorSpacing.extraLarge * 2 - MaterialTheme.timnorSpacing.extraSmall
    } else {
        MaterialTheme.timnorSpacing.extraLarge * 2 + MaterialTheme.timnorSpacing.small
    }
    val projectMarkerWidth = if (showProjectMarker) {
        MaterialTheme.timnorSpacing.small + MaterialTheme.timnorSpacing.extraSmall / 2
    } else MaterialTheme.timnorSpacing.small

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = groupVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(
            if (showProjectMarker) 0.dp else MaterialTheme.timnorSpacing.small,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showProjectMarker) Spacer(Modifier.width(homeRowSpacing))
        Text(
            text = hour.toString().padStart(2, '0'),
            modifier = if (showProjectMarker) Modifier else Modifier.width(MaterialTheme.timnorSpacing.extraLarge),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        if (showProjectMarker) Spacer(Modifier.width(homeRowSpacing))
        HorizontalDivider(
            modifier = Modifier.width(
                if (showProjectMarker) {
                    MaterialTheme.timnorSpacing.large + MaterialTheme.timnorSpacing.extraSmall / 4
                } else MaterialTheme.timnorSpacing.large,
            ),
            thickness = if (showProjectMarker) {
                MaterialTheme.timnorSpacing.extraSmall / 2
            } else DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (showProjectMarker) Spacer(Modifier.width(homeRowSpacing))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (showProjectMarker) 0.dp else MaterialTheme.timnorSpacing.small),
        ) {
            works.forEach { work ->
                Surface(
                    onClick = { onSelectWork(work) },
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = if (showProjectMarker) homeRowSpacing else 0.dp)
                        .heightIn(min = workCardMinHeight)
                        .testTag("work-entry-${work.id}"),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showProjectMarker) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxHeight().width(projectMarkerWidth)
                                    .testTag("work-entry-project-marker-${work.id}")
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f).padding(
                                start = if (showProjectMarker) projectMarkerWidth else MaterialTheme.timnorSpacing.small,
                                top = if (showProjectMarker) homeRowSpacing else MaterialTheme.timnorSpacing.small,
                                bottom = if (showProjectMarker) homeRowSpacing else MaterialTheme.timnorSpacing.small,
                            ),
                        ) {
                            val linkedName = linkedPlannorProjectName(work)
                            Text(
                                projectName(work),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (showEntryDetails) WorkTimelineTimeRange(work)
                            linkedName?.let { name ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.plannor_link_24),
                                        contentDescription = stringResource(Res.string.plannor_project_marker_description),
                                        tint = MaterialTheme.colorScheme.secondaryContainer,
                                    )
                                    Text(
                                        stringResource(Res.string.plannor_linked_project_label, name),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (work.description.isNotBlank()) {
                                Text(
                                    work.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            sharedWorkIndicators[work.id]?.let { indicator ->
                                SharedWorkSyncLabel(indicator)
                            }
                        }
                        val durationLabel = if (showEntryDetails && showDuration) {
                            workDurationLabel(work.time.toLong())
                        } else {
                            workTimelineDurationLabel(work.time, showDuration, localizedDecimalSeparator())
                        }
                        if (durationLabel != null) {
                            WorkTimelineDuration(
                                workId = work.id,
                                durationLabel = durationLabel,
                                payOre = payForWork(work),
                                modifier = Modifier.padding(
                                    start = if (showProjectMarker) homeRowSpacing else MaterialTheme.timnorSpacing.small,
                                    end = if (showProjectMarker) {
                                        MaterialTheme.timnorSpacing.small + MaterialTheme.timnorSpacing.extraSmall / 2
                                    } else MaterialTheme.timnorSpacing.small,
                                ),
                            )
                        }
                    }
                }
                if (showProjectMarker) Spacer(Modifier.width(MaterialTheme.timnorSpacing.extraSmall))
            }
        }
    }
}

@Composable
private fun WorkTimelineTimeRange(work: StoredWork) {
    val start = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds)
    val end = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds + work.time)
    Text(
        text = stringResource(Res.string.work_entry_time_range, start.hourAndMinute(), end.hourAndMinute()),
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun LocalDateTime.hourAndMinute(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
private fun WorkTimelineDuration(
    workId: Int,
    durationLabel: String,
    payOre: Long?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = durationLabel,
            modifier = Modifier.testTag("work-entry-duration-$workId"),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        payOre?.let {
            Text(
                text = workPayLabel(it),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
            )
        }
    }
}

internal fun formatMonthViewDuration(seconds: Int, decimalSeparator: Char = ','): String = when {
    seconds >= 3_600 -> {
        val tenths = (seconds.toLong() * 10 + 1_800) / 3_600
        "${tenths / 10}$decimalSeparator${tenths % 10}h"
    }
    seconds >= 60 -> {
        val tenths = (seconds.toLong() * 10 + 30) / 60
        "${tenths / 10}$decimalSeparator${tenths % 10}m"
    }
    else -> "$seconds s"
}

internal fun workTimelineDurationLabel(
    seconds: Int,
    showDuration: Boolean,
    decimalSeparator: Char = ',',
): String? = if (showDuration) formatMonthViewDuration(seconds, decimalSeparator) else null
