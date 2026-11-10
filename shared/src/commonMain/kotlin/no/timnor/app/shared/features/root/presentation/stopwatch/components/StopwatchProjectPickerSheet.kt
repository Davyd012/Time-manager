package no.timnor.app.shared.features.root.presentation.stopwatch.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.presentation.newProject.components.ProjectIndicator
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.delete
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.edit
import no.timnor.app.shared.generated.resources.manage_projects
import no.timnor.app.shared.generated.resources.no_projects
import no.timnor.app.shared.generated.resources.project_add
import no.timnor.app.shared.generated.resources.project_picker_title
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.generated.resources.stopwatch_project_locked
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StopwatchProjectPickerSheet(
    projects: List<StoredProject>,
    selectedProjectId: Int,
    error: TimnorHomeError?,
    onSelectProject: (Int) -> Unit,
    onEditProject: (StoredProject) -> Unit,
    onDeleteProject: (StoredProject, (Boolean) -> Unit) -> Unit,
    onCreateProject: () -> Unit,
    onManageProjects: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                Text(stringResource(Res.string.project_picker_title), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) }
            }
            if (projects.isEmpty()) {
                Text(stringResource(Res.string.no_projects), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    items(projects, key = StoredProject::id) { project ->
                        SwipeableStopwatchProjectRow(
                            project = project,
                            selected = project.id == selectedProjectId,
                            onSelect = { onSelectProject(project.id) },
                            onEdit = { onEditProject(project) },
                            onDelete = { onResult -> onDeleteProject(project, onResult) },
                        )
                    }
                }
            }
            if (error == TimnorHomeError.STORAGE || error == TimnorHomeError.ACTIVE_STOPWATCH_PROJECT) {
                Text(
                    stringResource(
                        if (error == TimnorHomeError.STORAGE) Res.string.storage_error
                        else Res.string.stopwatch_project_locked,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(onClick = onCreateProject) { Text(stringResource(Res.string.project_add)) }
            TextButton(onClick = onManageProjects) { Text(stringResource(Res.string.manage_projects)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableStopwatchProjectRow(
    project: StoredProject,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: ((Boolean) -> Unit) -> Unit,
) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState()
    var deleting by remember(project.id) { mutableStateOf(false) }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val scope = rememberCoroutineScope()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onEdit()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> deleting = true
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    LaunchedEffect(deleting) {
        if (deleting) {
            if (animationsEnabled) delay(180)
            onDelete { deleted ->
                if (!deleted) {
                    deleting = false
                    scope.launch { dismissState.reset() }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = !deleting,
        exit = if (animationsEnabled) fadeOut() + shrinkVertically() else androidx.compose.animation.ExitTransition.None,
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val editing = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            if (editing) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(MaterialTheme.timnorSpacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (editing) {
                        Icon(
                            painterResource(Res.drawable.edit),
                            contentDescription = stringResource(Res.string.edit),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } else {
                        Spacer(Modifier)
                    }
                    if (!editing) {
                        Icon(
                            painterResource(Res.drawable.delete),
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            },
            content = {
                Surface(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
                    ) {
                        ProjectIndicator(project.id)
                        Text(
                            project.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (selected) Icon(painterResource(Res.drawable.done), contentDescription = null)
                    }
                }
            },
        )
    }
}
