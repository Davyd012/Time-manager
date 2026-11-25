package no.timnor.app.shared.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.LocalDate
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.worksInCalendarMonth
import no.timnor.app.shared.domain.project.calendarMonthPay
import no.timnor.app.shared.domain.project.calendarProjectSelectionForApply
import no.timnor.app.shared.domain.project.filterProjects
import no.timnor.app.shared.domain.project.toggleProjectSelection
import no.timnor.app.shared.domain.project.toggleVisibleProjectSelection
import no.timnor.app.shared.features.root.presentation.home.components.monthName
import no.timnor.app.shared.features.root.presentation.home.components.workDurationLabel
import no.timnor.app.shared.features.root.presentation.home.components.workPayLabel
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.project_filter_apply
import no.timnor.app.shared.generated.resources.project_filter_no_results
import no.timnor.app.shared.generated.resources.project_filter_partial_pay
import no.timnor.app.shared.generated.resources.project_filter_reset
import no.timnor.app.shared.generated.resources.project_filter_selected_count
import no.timnor.app.shared.generated.resources.project_filter_search_hint
import no.timnor.app.shared.generated.resources.project_filter_select_all
import no.timnor.app.shared.generated.resources.project_filter_title
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectFilterSheet(
    projects: List<StoredProject>,
    works: List<StoredWork>,
    month: LocalDate,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedProjectIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    appliedProjectIds: Set<Int>?,
    onApply: (Set<Int>?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var draftProjectIds by remember {
        mutableStateOf(appliedProjectIds)
    }
    val allProjectIds = remember(projects) { projects.mapTo(mutableSetOf(), StoredProject::id) }
    val visibleProjects = remember(projects, query) {
        filterProjects(projects, query, StoredProject::name)
    }
    val visibleProjectIds = remember(visibleProjects) { visibleProjects.mapTo(mutableSetOf(), StoredProject::id) }
    val allVisibleSelected = visibleProjects.isNotEmpty() &&
        (draftProjectIds == null || visibleProjectIds.all { it in draftProjectIds.orEmpty() })
    val selectedCount = draftProjectIds?.intersect(allProjectIds)?.size ?: allProjectIds.size
    val projectTotals = remember(works, month, projects) {
        val projectsById = projects.associateBy(StoredProject::id)
        worksInCalendarMonth(works, month).groupBy(StoredWork::project).mapValues { (_, entries) ->
            entries.sumOf { it.time.toLong() } to calendarMonthPay(entries, projectsById)
        }
    }

    val spacing = MaterialTheme.timnorSpacing
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = spacing.large,
                end = spacing.large,
                bottom = spacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    stringResource(Res.string.project_filter_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("project-filter-cancel")) {
                    Text(stringResource(Res.string.cancel))
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("project-filter-search"),
                singleLine = true,
                label = { Text(stringResource(Res.string.project_filter_search_hint)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().testTag("project-filter-select-all").toggleable(
                    value = allVisibleSelected,
                    enabled = visibleProjects.isNotEmpty(),
                    role = Role.Checkbox,
                    onValueChange = {
                        draftProjectIds = toggleVisibleProjectSelection(
                            selectedIds = draftProjectIds,
                            visibleProjectIds = visibleProjectIds,
                            allProjectIds = allProjectIds,
                        )
                    },
                ).minimumInteractiveComponentSize().padding(horizontal = spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    stringResource(Res.string.project_filter_select_all),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "${monthName(month.month).replaceFirstChar { it.titlecase() }} ${month.year}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Checkbox(
                    checked = allVisibleSelected,
                    onCheckedChange = null,
                    enabled = visibleProjects.isNotEmpty(),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).testTag("project-filter-list"),
            ) {
                if (visibleProjects.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.project_filter_no_results),
                            modifier = Modifier.padding(vertical = spacing.large),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(visibleProjects, key = StoredProject::id) { project ->
                    val totals = projectTotals[project.id]
                    val pay = totals?.second
                    val payLabel = pay?.amountOre?.let { amount ->
                        val label = workPayLabel(amount)
                        if (pay.unratedSeconds > 0L) {
                            stringResource(Res.string.project_filter_partial_pay, label)
                        } else {
                            label
                        }
                    }
                    ProjectFilterRow(
                        projectId = project.id,
                        projectName = project.name,
                        linkedPlannorProjectName = linkedPlannorProjectNames[project.id],
                        syncIndicator = sharedProjectIndicators[project.id],
                        monthlyHoursLabel = workDurationLabel(totals?.first ?: 0L),
                        checked = draftProjectIds == null || project.id in draftProjectIds.orEmpty(),
                        onClick = {
                            draftProjectIds = toggleProjectSelection(draftProjectIds, project.id, allProjectIds)
                        },
                        monthlyPayLabel = payLabel,
                    )
                    HorizontalDivider()
                }
            }
            ProjectFilterActions(
                selectedCount = selectedCount,
                projectCount = allProjectIds.size,
                onReset = {
                    query = ""
                    draftProjectIds = null
                },
                onApply = {
                    onApply(calendarProjectSelectionForApply(draftProjectIds?.intersect(allProjectIds)))
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ProjectFilterActions(
    selectedCount: Int,
    projectCount: Int,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Text(
            stringResource(Res.string.project_filter_selected_count, selectedCount, projectCount),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
        )
        TextButton(onClick = onReset) { Text(stringResource(Res.string.project_filter_reset)) }
    }
    Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.project_filter_apply))
    }
}

internal fun formatHours(seconds: Long, decimalSeparator: Char): String {
    val unroundedHundredths = seconds / 36
    val remainder = seconds % 36
    val hundredths = when {
        remainder < 18 -> unroundedHundredths
        remainder > 18 -> unroundedHundredths + 1
        unroundedHundredths % 2L == 0L -> unroundedHundredths
        else -> unroundedHundredths + 1
    }
    val hours = hundredths / 100
    val fraction = hundredths % 100
    return when {
        fraction == 0L -> hours.toString()
        fraction % 10L == 0L -> "$hours$decimalSeparator${fraction / 10}"
        else -> "$hours$decimalSeparator${fraction.toString().padStart(2, '0')}"
    }
}
