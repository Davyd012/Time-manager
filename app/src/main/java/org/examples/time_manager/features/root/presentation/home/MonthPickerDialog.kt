package org.examples.time_manager.features.root.presentation.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.features.root.data.HomeIntent
import org.examples.time_manager.features.root.data.HomeIntent.ModifyExportProjects
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.arrowRightIcon
import org.examples.time_manager.ui.theme.doneIcon
import org.examples.time_manager.ui.theme.exportIcon
import org.examples.time_manager.ui.theme.spacing
import java.time.LocalDate

@Composable
fun MonthPickerDialog(
    onDismiss: (Int) -> Unit,
    onIntent: (HomeIntent) -> Unit,
    projectValues: List<Project>,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val spacing = MaterialTheme.spacing
    val months = stringArrayResource(R.array.months_array).toList()
    val initialDate = remember { LocalDate.now() }

    var selectedMonth by remember { mutableIntStateOf(initialDate.monthValue - 1) }
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var exportProjects by remember { mutableStateOf(emptyList<Project>()) }

    Dialog(
        onDismissRequest = { onDismiss(-1) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .padding(horizontal = spacing.medium),
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            tonalElevation = spacing.small,
        ) {
            Column(
                modifier = Modifier.padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                ExportHeader(
                    title = stringResource(R.string.export_to_excel_title),
                    subtitle = stringResource(R.string.export_to_excel_subtitle),
                )

                Text(
                    text = stringResource(R.string.projects_label),
                    style = typography.titleMedium,
                )
                ProjectSelectionRow(
                    projects = projectValues,
                    selectedProjects = exportProjects.map { it.id },
                    onProjectSelected = { projectId ->
                        val project = projectValues.first { it.id == projectId }
                        exportProjects = if (project in exportProjects) {
                            exportProjects.filterNot { it.id == projectId }
                        } else {
                            exportProjects + project
                        }
                        onIntent(ModifyExportProjects(project))
                    },
                )

                Text(
                    text = stringResource(R.string.month_label),
                    style = typography.titleMedium,
                )
                YearSelector(
                    year = selectedYear,
                    onPreviousYear = { selectedYear-- },
                    onNextYear = { selectedYear++ },
                )
                MonthGrid(
                    months = months,
                    selectedMonth = selectedMonth,
                    onMonthSelected = { selectedMonth = it },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    OutlinedButton(
                        onClick = { onDismiss(-1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { onDismiss(selectedMonth) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        ),
                    ) {
                        Text(stringResource(R.string.export_btn))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportHeader(
    title: String,
    subtitle: String,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Icon(
            imageVector = exportIcon(),
            tint = colors.secondary,
            contentDescription = stringResource(R.string.export_action),
            modifier = Modifier.size(35.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectSelectionRow(
    projects: List<Project>,
    selectedProjects: List<Int>,
    onProjectSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        projects.forEach { project ->
            val selected = project.id in selectedProjects
            FilterChip(
                selected = selected,
                onClick = { onProjectSelected(project.id) },
                label = {
                    Text(
                        text = project.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = doneIcon(),
                            contentDescription = null,
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun YearSelector(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedIconButton(onClick = onPreviousYear) {
            Icon(
                imageVector = arrowLeftIcon(),
                contentDescription = stringResource(R.string.previous_year_cd),
            )
        }
        Text(year.toString(), style = MaterialTheme.typography.headlineSmall)
        OutlinedIconButton(onClick = onNextYear) {
            Icon(
                imageVector = arrowRightIcon(),
                contentDescription = stringResource(R.string.next_year_cd),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    months: List<String>,
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        months.chunked(3).forEachIndexed { rowIndex, rowMonths ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                rowMonths.forEachIndexed { columnIndex, month ->
                    val monthIndex = rowIndex * 3 + columnIndex
                    MonthButton(
                        month = month,
                        selected = monthIndex == selectedMonth,
                        onClick = { onMonthSelected(monthIndex) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthButton(
    month: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ),
        ) {
            Text(month)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            modifier = modifier,
        ) {
            Text(month)
        }
    }
}
