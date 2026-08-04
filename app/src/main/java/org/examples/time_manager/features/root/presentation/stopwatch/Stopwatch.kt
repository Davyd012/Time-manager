package org.examples.time_manager.features.root.presentation.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.features.root.StopwatchViewModel
import org.examples.time_manager.features.root.data.StopwatchIntent.ModifyProject
import org.examples.time_manager.features.root.data.StopwatchIntent.SelectProject
import org.examples.time_manager.features.root.data.StopwatchIntent.UpdateTimer
import org.examples.time_manager.features.root.data.TimerStates
import org.examples.time_manager.features.root.presentation.stopwatch.components.SwipeToDeleteContainer
import org.examples.time_manager.features.root.presentation.utils.normalizeTime
import org.examples.time_manager.ui.theme.addIcon
import org.examples.time_manager.ui.theme.pauseTimerIcon
import org.examples.time_manager.ui.theme.playIcon

@Composable
fun Stopwatch(vm: StopwatchViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val styles = MaterialTheme.typography

    val state by vm.state.collectAsStateWithLifecycle()
    val time = state.elapsedSeconds.toDouble()
    val projects = state.projects

    var showInputNewProject by remember { mutableStateOf(false) }

    var currentProject by remember { mutableStateOf<Project?>(null) }

    if (showInputNewProject) {
        NewProjectInput(
            onDismiss = {
                showInputNewProject = false
                currentProject = null
            },
            onIntent = vm::onIntent,
            project = currentProject,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(colors.primary)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                normalizeTime(time),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary
            )
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            projects.forEach {
                SwipeToDeleteContainer(
                    color = colors.error,
                    removeAction = {
                        vm.onIntent(
                            ModifyProject(project = it, delete = true)
                        )
                    },
                    modifyProject = {
                        showInputNewProject = true
                        currentProject = it
                    },
                    content = {
                        Text(
                            text = it.name,
                            style = styles.titleMedium.copy(color = if (it.id == state.selectedProject) colors.onPrimaryContainer else colors.onSecondaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp, horizontal = 15.dp)
                                .clip(
                                    MaterialTheme.shapes.small
                                )
                                .clickable { vm.onIntent(SelectProject(value = it.id)) }
                                .background(if (it.id == state.selectedProject) colors.primaryContainer else colors.secondaryContainer)
                                .padding(15.dp),
                        )
                    },
                )
            }
        }

        val imageVector =
            if (state.isRunning) pauseTimerIcon() else playIcon()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(70.dp)
        ) {
            Icon(
                imageVector, tint = colors.onTertiaryContainer, contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable {
                        if (state.isRunning) {
                            vm.onIntent(UpdateTimer(TimerStates.PauseStopwatch))
                            return@clickable
                        }
                        vm.onIntent(UpdateTimer(TimerStates.StartStopwatch))
                    }
                    .background(colors.tertiaryContainer)
                    .padding(12.dp)
                    .size(50.dp),
            )
//            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.done_btn),
                style = styles.titleMedium.copy(
                    colors.onSecondary,
                    fontWeight = FontWeight.W700,
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.secondary)
                    .clickable {
                        vm.onIntent(UpdateTimer(TimerStates.SaveResult))
                    }
                    .padding(vertical = 17.dp, horizontal = 30.dp),
            )
            Spacer(modifier = Modifier.width(15.dp))

            Icon(
                imageVector = addIcon(),
                contentDescription = null,
                tint = colors.onTertiary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(colors.tertiary)
//                    .padding(10.dp)
                    .size(50.dp)
                    .clickable {
                        showInputNewProject = true
                    },
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
