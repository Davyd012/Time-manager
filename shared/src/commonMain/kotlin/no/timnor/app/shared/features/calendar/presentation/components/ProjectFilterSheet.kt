package no.timnor.app.shared.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
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
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.domain.project.calendarProjectSelectionForApply
import no.timnor.app.shared.domain.project.toggleProjectSelection
import no.timnor.app.shared.domain.project.toggleVisibleProjectSelection
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.back_to_home
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.project_filter_search_hint
import no.timnor.app.shared.generated.resources.project_filter_select_all
import no.timnor.app.shared.generated.resources.project_filter_title
import no.timnor.app.shared.generated.resources.project_filter_no_results
import no.timnor.app.shared.generated.resources.project_hours
import no.timnor.app.shared.platform.locale.localizedDecimalSeparator
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectFilterSheet(
    projects: List<StoredProject>,
    works: List<StoredWork>,
    month: LocalDate,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedProjectIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    selectedProjectIds: Set<Int>?,
    onApply: (Set<Int>?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selection by remember(selectedProjectIds) {
        mutableStateOf(calendarProjectSelectionForApply(selectedProjectIds))
    }
    val allProjectIds = remember(projects) { projects.mapTo(mutableSetOf(), StoredProject::id) }
    val visibleProjects = remember(projects, query) {
        projects.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val visibleProjectIds = remember(visibleProjects) { visibleProjects.mapTo(mutableSetOf(), StoredProject::id) }
    val allVisibleSelected = visibleProjects.isNotEmpty() &&
        (selection == null || visibleProjectIds.all { it in selection.orEmpty() })
    val projectSeconds = remember(works, month) {
        works.asSequence()
            .filter { work ->
                val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
                date.year == month.year && date.month == month.month
            }
            .groupingBy(StoredWork::project)
            .fold(0L) { total, work -> total + work.time }
    }

    val applyAndDismiss = {
        onApply(calendarProjectSelectionForApply(selection))
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = applyAndDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.timnorSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = applyAndDismiss,
                    modifier = Modifier.testTag("project-filter-back"),
                ) {
                    Icon(painterResource(Res.drawable.arrow_back), contentDescription = stringResource(Res.string.back_to_home))
                }
                Text(
                    stringResource(Res.string.project_filter_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = applyAndDismiss) {
                    Text(stringResource(Res.string.done))
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.project_filter_search_hint)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().testTag("project-filter-select-all").toggleable(
                    value = allVisibleSelected,
                    enabled = visibleProjects.isNotEmpty(),
                    role = Role.Checkbox,
                    onValueChange = {
                        selection = toggleVisibleProjectSelection(
                            selectedIds = selection,
                            visibleProjectIds = visibleProjectIds,
                            allProjectIds = allProjectIds,
                        )
                    },
                ).padding(horizontal = MaterialTheme.timnorSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.project_filter_select_all), style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = allVisibleSelected,
                    onCheckedChange = null,
                    enabled = visibleProjects.isNotEmpty(),
                )
            }
            if (visibleProjects.isEmpty()) {
                Text(
                    stringResource(Res.string.project_filter_no_results),
                    modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.timnorSpacing.large),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    items(visibleProjects, key = StoredProject::id) { project ->
                        val seconds = projectSeconds[project.id] ?: 0L
                        ProjectFilterRow(
                            projectId = project.id,
                            projectName = project.name,
                            linkedPlannorProjectName = linkedPlannorProjectNames[project.id],
                            syncIndicator = sharedProjectIndicators[project.id],
                            monthlyHoursLabel = stringResource(
                                Res.string.project_hours,
                                formatHours(seconds, localizedDecimalSeparator()),
                            ),
                            checked = selection == null || project.id in selection.orEmpty(),
                            onClick = {
                                selection = toggleProjectSelection(selection, project.id, allProjectIds)
                            },
                        )
                    }
                }
            }
        }
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
