package org.examples.time_manager.features.root.presentation.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.features.root.StopwatchViewModel
import org.examples.time_manager.features.root.data.StopwatchIntent
import org.examples.time_manager.features.root.data.StopwatchIntent.ModifyProject
import org.examples.time_manager.features.root.data.StopwatchIntent.SelectProject
import org.examples.time_manager.features.root.data.StopwatchIntent.UpdateTimer
import org.examples.time_manager.features.root.data.StopwatchUiState
import org.examples.time_manager.features.root.data.TimerStates
import org.examples.time_manager.features.root.presentation.stopwatch.components.ProjectPickerSheet
import org.examples.time_manager.features.root.presentation.stopwatch.components.SwipeToDeleteContainer
import org.examples.time_manager.features.root.presentation.utils.normalizeTime
import org.examples.time_manager.ui.theme.addIcon
import org.examples.time_manager.ui.theme.arrowRightIcon
import org.examples.time_manager.ui.theme.doneIcon
import org.examples.time_manager.ui.theme.pauseTimerIcon
import org.examples.time_manager.ui.theme.playIcon
import org.examples.time_manager.ui.theme.workIcon
import org.examples.time_manager.ui.theme.spacing

@Composable
fun Stopwatch(vm: StopwatchViewModel, modifier: Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showInputNewProject by remember { mutableStateOf(false) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var currentProject by remember { mutableStateOf<Project?>(null) }
    var selectCreatedProject by remember { mutableStateOf(false) }
    var projectIdsBeforeCreation by remember { mutableStateOf(emptySet<Int>()) }

    LaunchedEffect(state.projects, selectCreatedProject) {
        if (selectCreatedProject) {
            state.projects
                .firstOrNull { it.id !in projectIdsBeforeCreation }
                ?.let { createdProject ->
                    vm.onIntent(SelectProject(createdProject.id))
                    selectCreatedProject = false
                }
        }
    }

    StopwatchContent(
        state = state,
        modifier = modifier,
        onIntent = vm::onIntent,
        showInputNewProject = showInputNewProject,
        currentProject = currentProject,
        showProjectPicker = showProjectPicker,
        onShowNewProject = { project ->
            currentProject = project
            if (project == null) {
                projectIdsBeforeCreation = state.projects.map(Project::id).toSet()
                selectCreatedProject = true
            } else {
                selectCreatedProject = false
            }
            showProjectPicker = false
            showInputNewProject = true
        },
        onDismissNewProject = {
            showInputNewProject = false
            currentProject = null
        },
        onShowProjectPicker = { showProjectPicker = true },
        onDismissProjectPicker = { showProjectPicker = false },
    )
}

@Composable
fun StopwatchContent(
    state: StopwatchUiState,
    modifier: Modifier,
    onIntent: (StopwatchIntent) -> Unit,
    showInputNewProject: Boolean = false,
    currentProject: Project? = null,
    showProjectPicker: Boolean = false,
    onShowNewProject: (Project?) -> Unit = {},
    onDismissNewProject: () -> Unit = {},
    onShowProjectPicker: () -> Unit = {},
    onDismissProjectPicker: () -> Unit = {},
) {
    val spacing = MaterialTheme.spacing
    val project = state.projects.firstOrNull { it.id == state.selectedProject }
    val hasActiveSession = state.elapsedSeconds > 0
    val isIdle = !hasActiveSession
    val statusRes = when {
        state.isRunning -> R.string.stopwatch_running_status
        hasActiveSession -> R.string.stopwatch_paused_status
        else -> R.string.stopwatch_idle_status
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(.5f))
                Text(
                    text = normalizeTime(state.elapsedSeconds.toDouble()),
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = stringResource(statusRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(.5f))

                ProjectCard(
                    project = project,
                    enabled = !hasActiveSession,
                    onClick = onShowProjectPicker,
                )

                Spacer(modifier = Modifier.weight(1f))

                StopwatchAction(
                    isRunning = state.isRunning,
                    isIdle = isIdle,
                    onPrimaryAction = {
                        if (state.isRunning) {
                            onIntent(UpdateTimer(TimerStates.PauseStopwatch))
                        } else if (project == null) {
                            onShowProjectPicker()
                        } else {
                            onIntent(UpdateTimer(TimerStates.StartStopwatch))
                        }
                    },
                    onDone = { onIntent(UpdateTimer(TimerStates.SaveResult)) },
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        FloatingActionButton(
            onClick = { onShowNewProject(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = spacing.medium, bottom = spacing.medium),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                imageVector = addIcon(),
                contentDescription = stringResource(R.string.new_project_title),
            )
        }
    }

    if (showProjectPicker) {
        ProjectPickerSheet(
            projects = state.projects,
            selectedProjectId = state.selectedProject,
            onSelect = { projectId ->
                onIntent(SelectProject(projectId))
                onDismissProjectPicker()
            },
            onCreateProject = { onShowNewProject(null) },
            onEditProject = onShowNewProject,
            onDeleteProject = { project ->
                onIntent(ModifyProject(project = project, delete = true))
            },
            onDismiss = onDismissProjectPicker,
        )
    }

    if (showInputNewProject) {
        NewProjectInput(
            onDismiss = onDismissNewProject,
            onIntent = onIntent,
            project = currentProject,
        )
    }
}

@Composable
private fun ProjectCard(
    project: Project?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = colors.outlineVariant,
        ),
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = workIcon(),
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(
                    text = project?.name ?: stringResource(R.string.select_project_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(
                        if (project == null) R.string.select_project_before_start
                        else R.string.active_project_label,
                    ),
                )
            },
            trailingContent = {
                Icon(
                    imageVector = arrowRightIcon(),
                    contentDescription = null,
                )
            },
            tonalElevation = spacing.small,
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = colors.surface,
            ),
        )
    }
}

@Composable
private fun StopwatchAction(
    isRunning: Boolean,
    isIdle: Boolean,
    onPrimaryAction: () -> Unit,
    onDone: () -> Unit,
) {
    val actionLabel = stringResource(
        when {
            isRunning -> R.string.pause_action
            isIdle -> R.string.start_action
            else -> R.string.resume_action
        },
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LargeFloatingActionButton(
            onClick = onPrimaryAction,
            containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
        ) {
            AnimatedContent(
                targetState = isRunning,
                label = "stopwatchActionIcon",
            ) { running ->
                Icon(
                    imageVector = if (running) pauseTimerIcon() else playIcon(),
                    contentDescription = actionLabel,
                    modifier = Modifier
                        .size(MaterialTheme.spacing.extraLarge),
                )
            }
        }
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
        )
        AnimatedVisibility(
            visible = !isIdle,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TextButton(onClick = onDone) {
                Icon(imageVector = doneIcon(), contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.done_btn))
            }
        }
    }
}
