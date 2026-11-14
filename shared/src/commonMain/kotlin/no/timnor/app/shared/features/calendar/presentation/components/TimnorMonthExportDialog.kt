package no.timnor.app.shared.features.calendar.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.generated.resources.arrow_right
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.export_btn
import no.timnor.app.shared.generated.resources.export_spreadsheet
import no.timnor.app.shared.generated.resources.export_to_excel_subtitle
import no.timnor.app.shared.generated.resources.export_to_excel_title
import no.timnor.app.shared.generated.resources.icon_import_export
import no.timnor.app.shared.generated.resources.month_label
import no.timnor.app.shared.generated.resources.next_year_cd
import no.timnor.app.shared.generated.resources.previous_year_cd
import no.timnor.app.shared.generated.resources.projects_label
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
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
    val previousYearLabel = stringResource(Res.string.previous_year_cd)
    val nextYearLabel = stringResource(Res.string.next_year_cd)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .padding(horizontal = MaterialTheme.timnorSpacing.medium),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = MaterialTheme.timnorSpacing.small,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.timnorSpacing.large)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_import_export),
                        contentDescription = stringResource(Res.string.export_spreadsheet),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(MaterialTheme.timnorSpacing.extraLarge)
                            .testTag("month-export-header-icon"),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                        Text(stringResource(Res.string.export_to_excel_title), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(Res.string.export_to_excel_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(stringResource(Res.string.projects_label), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    projects.forEach { project ->
                        FilterChip(
                            selected = project.id in selectedProjectIds,
                            modifier = Modifier.testTag("month-export-project-${project.id}"),
                            onClick = {
                                selectedProjectIds = if (project.id in selectedProjectIds) {
                                    selectedProjectIds - project.id
                                } else {
                                    selectedProjectIds + project.id
                                }
                            },
                            leadingIcon = if (project.id in selectedProjectIds) {
                                {
                                    Icon(painterResource(Res.drawable.done), contentDescription = null)
                                }
                            } else null,
                            label = { Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
                Text(stringResource(Res.string.month_label), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedIconButton(
                        onClick = { selectedYear-- },
                        modifier = Modifier
                            .testTag("month-export-previous-year")
                            .semantics { contentDescription = previousYearLabel },
                    ) {
                        Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
                    }
                    Text(
                        selectedYear.toString(),
                        modifier = Modifier.testTag("month-export-year"),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    OutlinedIconButton(
                        onClick = { selectedYear++ },
                        modifier = Modifier
                            .testTag("month-export-next-year")
                            .semantics { contentDescription = nextYearLabel },
                    ) {
                        Icon(painterResource(Res.drawable.arrow_right), contentDescription = null)
                    }
                }
                months.chunked(3).forEachIndexed { row, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                    ) {
                        rowMonths.forEachIndexed { column, monthName ->
                            val index = row * 3 + column
                            if (selectedMonth == index) {
                                Button(
                                    modifier = Modifier.weight(1f).testTag("month-export-option-$index"),
                                    onClick = { selectedMonth = index },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    contentPadding = PaddingValues(horizontal = MaterialTheme.timnorSpacing.small),
                                ) { Text(monthName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            } else {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f).testTag("month-export-option-$index"),
                                    onClick = { selectedMonth = index },
                                    contentPadding = PaddingValues(horizontal = MaterialTheme.timnorSpacing.small),
                                ) { Text(monthName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = {
                            onExport(selectedYear, selectedMonth + 1, selectedProjectIds.takeIf { it.isNotEmpty() })
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) { Text(stringResource(Res.string.export_btn)) }
                }
            }
        }
    }
}
