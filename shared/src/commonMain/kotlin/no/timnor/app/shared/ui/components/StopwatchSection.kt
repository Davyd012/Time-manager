package no.timnor.app.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.stopwatch_cancel
import no.timnor.app.shared.generated.resources.stopwatch_no_projects
import no.timnor.app.shared.generated.resources.stopwatch_pause
import no.timnor.app.shared.generated.resources.stopwatch_save
import no.timnor.app.shared.generated.resources.stopwatch_start
import no.timnor.app.shared.generated.resources.stopwatch_title
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StopwatchSection(
    projects: List<StoredProject>,
    elapsedSeconds: Int,
    isRunning: Boolean,
    selectedProjectId: Int,
    onSelectProject: (Int) -> Unit,
    onStartPause: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    var projectMenuExpanded by remember { mutableStateOf(false) }
    val elapsed = elapsedSeconds.coerceAtLeast(0)
    fun twoDigits(value: Int) = value.toString().padStart(2, '0')
    val timeLabel = "${twoDigits(elapsed / 3_600)}:${twoDigits(elapsed / 60 % 60)}:${twoDigits(elapsed % 60)}"

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(stringResource(Res.string.stopwatch_title), style = MaterialTheme.typography.titleLarge)
        Text(timeLabel, style = MaterialTheme.typography.headlineMedium)
        if (projects.isEmpty()) {
            Text(stringResource(Res.string.stopwatch_no_projects))
        } else {
            ProjectSelector(
                projects = projects,
                selectedProjectId = selectedProjectId,
                onProjectSelected = onSelectProject,
                expanded = projectMenuExpanded,
                onExpandedChange = { projectMenuExpanded = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                Button(onClick = onStartPause) {
                    Text(stringResource(if (isRunning) Res.string.stopwatch_pause else Res.string.stopwatch_start))
                }
                Button(onClick = onCancel, enabled = elapsedSeconds > 0) {
                    Text(stringResource(Res.string.stopwatch_cancel))
                }
                Button(onClick = onSave, enabled = elapsedSeconds > 0) {
                    Text(stringResource(Res.string.stopwatch_save))
                }
            }
        }
    }
}
