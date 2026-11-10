package no.timnor.app.shared.features.calendar.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_month_april
import no.timnor.app.shared.generated.resources.calendar_month_august
import no.timnor.app.shared.generated.resources.calendar_month_december
import no.timnor.app.shared.generated.resources.calendar_month_february
import no.timnor.app.shared.generated.resources.calendar_month_january
import no.timnor.app.shared.generated.resources.calendar_month_july
import no.timnor.app.shared.generated.resources.calendar_month_june
import no.timnor.app.shared.generated.resources.calendar_month_march
import no.timnor.app.shared.generated.resources.calendar_month_may
import no.timnor.app.shared.generated.resources.calendar_month_november
import no.timnor.app.shared.generated.resources.calendar_month_october
import no.timnor.app.shared.generated.resources.calendar_month_september
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.export_btn
import no.timnor.app.shared.generated.resources.export_to_excel_subtitle
import no.timnor.app.shared.generated.resources.export_to_excel_title
import no.timnor.app.shared.generated.resources.month_label
import no.timnor.app.shared.generated.resources.next_year_cd
import no.timnor.app.shared.generated.resources.previous_year_cd
import no.timnor.app.shared.generated.resources.projects_label
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TimnorMonthExportDialog(
    projects: List<StoredProject>,
    onDismiss: () -> Unit,
    onExport: (year: Int, month: Int, projectIds: Set<Int>?) -> Unit,
) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var selectedMonth by remember { mutableIntStateOf(now.month.ordinal) }
    var selectedYear by remember { mutableIntStateOf(now.year) }
    var selectedProjectIds by remember { mutableStateOf(emptySet<Int>()) }
    val months = listOf(
        stringResource(Res.string.calendar_month_january),
        stringResource(Res.string.calendar_month_february),
        stringResource(Res.string.calendar_month_march),
        stringResource(Res.string.calendar_month_april),
        stringResource(Res.string.calendar_month_may),
        stringResource(Res.string.calendar_month_june),
        stringResource(Res.string.calendar_month_july),
        stringResource(Res.string.calendar_month_august),
        stringResource(Res.string.calendar_month_september),
        stringResource(Res.string.calendar_month_october),
        stringResource(Res.string.calendar_month_november),
        stringResource(Res.string.calendar_month_december),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                Text(stringResource(Res.string.export_to_excel_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(Res.string.export_to_excel_subtitle), style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
            ) {
                Text(stringResource(Res.string.projects_label), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    projects.forEach { project ->
                        FilterChip(
                            selected = project.id in selectedProjectIds,
                            onClick = {
                                selectedProjectIds = if (project.id in selectedProjectIds) {
                                    selectedProjectIds - project.id
                                } else {
                                    selectedProjectIds + project.id
                                }
                            },
                            label = { Text(project.name) },
                        )
                    }
                }
                Text(stringResource(Res.string.month_label), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { selectedYear-- }) {
                        Text("‹ ${stringResource(Res.string.previous_year_cd)}")
                    }
                    Text(selectedYear.toString(), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { selectedYear++ }) {
                        Text("${stringResource(Res.string.next_year_cd)} ›")
                    }
                }
                months.chunked(3).forEachIndexed { row, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                    ) {
                        rowMonths.forEachIndexed { column, monthName ->
                            val index = row * 3 + column
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedMonth == index,
                                onClick = { selectedMonth = index },
                                label = { Text(monthName) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedYear, selectedMonth + 1, selectedProjectIds.takeIf { it.isNotEmpty() }) },
            ) { Text(stringResource(Res.string.export_btn)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(end = MaterialTheme.timnorSpacing.small)) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
