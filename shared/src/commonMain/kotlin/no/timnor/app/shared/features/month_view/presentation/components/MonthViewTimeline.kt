package no.timnor.app.shared.features.month_view.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.unknown_project
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MonthViewTimeline(
    works: List<StoredWork>,
    projects: List<StoredProject>,
    modifier: Modifier = Modifier,
    onSelectWork: (StoredWork) -> Unit,
) {
    val unknownProject = stringResource(Res.string.unknown_project)
    val groupedWorks = remember(works) { monthViewWorkGroups(works).toList() }
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            groupedWorks.forEach { (hour, entries) ->
                item(key = "hour-$hour") {
                    MonthViewTimelineGroup(
                        hour = hour,
                        works = entries,
                        projectName = { work -> projectNames[work.project] ?: unknownProject },
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
    onSelectWork: (StoredWork) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .padding(vertical = MaterialTheme.timnorSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hour.toString().padStart(2, '0'),
            modifier = Modifier.width(MaterialTheme.timnorSpacing.extraLarge),
            style = MaterialTheme.typography.titleMedium,
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            works.forEach { work ->
                Surface(
                    onClick = { onSelectWork(work) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.timnorSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                projectName(work),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (work.description.isNotBlank()) {
                                Text(
                                    work.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        val start = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds)
                        Text(
                            text = "${start.hour.toString().padStart(2, '0')}:${start.minute.toString().padStart(2, '0')} · ${formatMonthViewDuration(work.time)}",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

internal fun formatMonthViewDuration(seconds: Int): String = when {
    seconds >= 3_600 -> {
        val tenths = (seconds.toLong() * 10 + 1_800) / 3_600
        "${tenths / 10},${tenths % 10}h"
    }
    seconds >= 60 -> {
        val tenths = (seconds.toLong() * 10 + 300) / 600
        "${tenths / 10},${tenths % 10}m"
    }
    else -> "$seconds s"
}
