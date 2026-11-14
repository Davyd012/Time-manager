package no.timnor.app.shared.features.root.presentation.stopwatch.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.features.shared_hours.domain.PlannorTaskOption
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.features.shared_hours.presentation.SharedWorkSyncLabel
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.domain.stopwatch.canChangeStopwatchProject
import no.timnor.app.shared.domain.time.formatStopwatchTime
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.active_project_label
import no.timnor.app.shared.generated.resources.arrow_right
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.pause
import no.timnor.app.shared.generated.resources.play
import no.timnor.app.shared.generated.resources.select_project_before_start
import no.timnor.app.shared.generated.resources.select_project_title
import no.timnor.app.shared.generated.resources.stopwatch_idle_status
import no.timnor.app.shared.generated.resources.stopwatch_pause
import no.timnor.app.shared.generated.resources.stopwatch_paused_status
import no.timnor.app.shared.generated.resources.stopwatch_running_status
import no.timnor.app.shared.generated.resources.stopwatch_start
import no.timnor.app.shared.generated.resources.stopwatch_resume
import no.timnor.app.shared.generated.resources.work
import no.timnor.app.shared.generated.resources.plannor_linked_project_label
import no.timnor.app.shared.generated.resources.work_task_optional
import no.timnor.app.shared.generated.resources.work_task_none
import no.timnor.app.shared.ui.theme.timnorSpacing
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StopwatchSection(
    projects: List<StoredProject>,
    elapsedSeconds: Int,
    isRunning: Boolean,
    selectedProjectId: Int,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedProjectIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    plannorTasks: List<PlannorTaskOption> = emptyList(),
    selectedPlannorTaskId: Int? = null,
    error: TimnorHomeError?,
    onSelectProject: (Int) -> Unit,
    onSelectPlannorTask: (Int?) -> Unit,
    onStartPause: () -> Unit,
    onSave: () -> Unit,
    onCreateProject: () -> Unit,
    onEditProject: (StoredProject) -> Unit,
    onDeleteProject: (StoredProject, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProjectPicker by remember { mutableStateOf(false) }
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val elapsed = elapsedSeconds.coerceAtLeast(0)
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val status = when {
        isRunning -> Res.string.stopwatch_running_status
        elapsed > 0 -> Res.string.stopwatch_paused_status
        else -> Res.string.stopwatch_idle_status
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.5f))
        Text(
        text = formatStopwatchTime(elapsed),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = stringResource(status),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(0.5f))
        Card(
            onClick = { showProjectPicker = true },
            enabled = canChangeStopwatchProject(elapsed),
            modifier = Modifier.fillMaxWidth().testTag("stopwatch-project-picker"),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = selectedProject?.name ?: stringResource(Res.string.select_project_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = stringResource(
                                if (selectedProject == null) Res.string.select_project_before_start
                                else Res.string.active_project_label,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        selectedProject?.let { linkedPlannorProjectNames[it.id] }?.let { linkedName ->
                            Text(
                                stringResource(Res.string.plannor_linked_project_label, linkedName),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        selectedProject?.let { sharedProjectIndicators[it.id] }?.let { indicator ->
                            SharedWorkSyncLabel(indicator)
                        }
                    }
                },
                leadingContent = {
                    Icon(
                    painter = painterResource(Res.drawable.work),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
        if (selectedProject != null && linkedPlannorProjectNames.containsKey(selectedProject.id) && plannorTasks.isNotEmpty()) {
            StopwatchTaskPicker(
                tasks = plannorTasks,
                selectedTaskId = selectedPlannorTaskId,
                onSelectTask = onSelectPlannorTask,
            )
        }
        Spacer(Modifier.weight(1f))
        val primaryAction = when {
            isRunning -> stringResource(Res.string.stopwatch_pause)
            elapsed > 0 -> stringResource(Res.string.stopwatch_resume)
            else -> stringResource(Res.string.stopwatch_start)
        }
        LargeFloatingActionButton(
            onClick = { if (selectedProject == null) showProjectPicker = true else onStartPause() },
            containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
        ) {
            if (animationsEnabled) {
                AnimatedContent(targetState = isRunning, label = "stopwatch-action-icon") { running ->
                    Icon(
                        painter = painterResource(if (running) Res.drawable.pause else Res.drawable.play),
                        contentDescription = primaryAction,
                        modifier = Modifier.size(MaterialTheme.timnorSpacing.extraLarge),
                    )
                }
            } else {
                Icon(
                    painter = painterResource(if (isRunning) Res.drawable.pause else Res.drawable.play),
                    contentDescription = primaryAction,
                    modifier = Modifier.size(MaterialTheme.timnorSpacing.extraLarge),
                )
            }
        }
        Text(
            primaryAction,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = MaterialTheme.timnorSpacing.small),
        )
        if (animationsEnabled) AnimatedVisibility(visible = elapsed > 0, enter = fadeIn(), exit = fadeOut()) {
            TextButton(onClick = onSave) {
                Icon(painter = painterResource(Res.drawable.done), contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                Text(stringResource(Res.string.done))
            }
        } else if (elapsed > 0) {
            TextButton(onClick = onSave) {
                Icon(painter = painterResource(Res.drawable.done), contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                Text(stringResource(Res.string.done))
            }
        }
        Spacer(Modifier.weight(1f))
    }

    if (showProjectPicker) {
        StopwatchProjectPickerSheet(
            projects = projects,
            selectedProjectId = selectedProjectId,
            linkedPlannorProjectNames = linkedPlannorProjectNames,
            sharedProjectIndicators = sharedProjectIndicators,
            error = error,
            onSelectProject = { projectId ->
                onSelectProject(projectId)
                showProjectPicker = false
            },
            onEditProject = { project ->
                showProjectPicker = false
                onEditProject(project)
            },
            onDeleteProject = onDeleteProject,
            onCreateProject = {
                showProjectPicker = false
                onCreateProject()
            },
            onDismiss = { showProjectPicker = false },
        )
    }
}

@Composable
private fun StopwatchTaskPicker(
    tasks: List<PlannorTaskOption>,
    selectedTaskId: Int?,
    onSelectTask: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTask = tasks.firstOrNull { it.taskId == selectedTaskId }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedTask?.headline ?: stringResource(Res.string.work_task_optional))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.work_task_none)) },
                onClick = { onSelectTask(null); expanded = false },
            )
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = { Text(task.headline) },
                    onClick = { onSelectTask(task.taskId); expanded = false },
                )
            }
        }
    }
}
