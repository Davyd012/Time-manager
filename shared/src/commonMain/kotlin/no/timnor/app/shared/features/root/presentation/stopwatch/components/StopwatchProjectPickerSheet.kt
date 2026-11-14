package no.timnor.app.shared.features.root.presentation.stopwatch.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.features.shared_hours.presentation.SharedWorkSyncLabel
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.delete
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.delete_project_accessibility
import no.timnor.app.shared.generated.resources.edit
import no.timnor.app.shared.generated.resources.edit_project_accessibility
import no.timnor.app.shared.generated.resources.icon_add
import no.timnor.app.shared.generated.resources.no_projects
import no.timnor.app.shared.generated.resources.project_add
import no.timnor.app.shared.generated.resources.project_picker_title
import no.timnor.app.shared.generated.resources.plannor_linked_project_label
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
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedProjectIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    error: TimnorHomeError?,
    onSelectProject: (Int) -> Unit,
    onEditProject: (StoredProject) -> Unit,
    onDeleteProject: (StoredProject, (Boolean) -> Unit) -> Unit,
    onCreateProject: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.timnorSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            Text(stringResource(Res.string.project_picker_title), style = MaterialTheme.typography.headlineSmall)
            if (projects.isEmpty()) {
                Text(stringResource(Res.string.no_projects), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = MaterialTheme.timnorSpacing.extraLarge * 12),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    items(projects, key = StoredProject::id) { project ->
                        SwipeableStopwatchProjectRow(
                            project = project,
                            selected = project.id == selectedProjectId,
                            linkedPlannorProjectName = linkedPlannorProjectNames[project.id],
                            sharedWorkIndicator = sharedProjectIndicators[project.id],
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
            HorizontalDivider()
            TextButton(
                onClick = onCreateProject,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(painterResource(Res.drawable.icon_add), contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                Text(stringResource(Res.string.project_add))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableStopwatchProjectRow(
    project: StoredProject,
    selected: Boolean,
    linkedPlannorProjectName: String?,
    sharedWorkIndicator: SharedWorkSyncIndicator?,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: ((Boolean) -> Unit) -> Unit,
) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState()
    var deleting by remember(project.id) { mutableStateOf(false) }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val scope = rememberCoroutineScope()
    val editActionLabel = stringResource(Res.string.edit_project_accessibility, project.name)
    val deleteActionLabel = stringResource(Res.string.delete_project_accessibility, project.name)

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
            if (animationsEnabled) delay(500)
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
        exit = if (animationsEnabled) {
            fadeOut(tween(500)) + shrinkVertically(tween(500))
        } else androidx.compose.animation.ExitTransition.None,
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Row(
                    modifier = Modifier.fillMaxSize()
                        .padding(MaterialTheme.timnorSpacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.edit),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Icon(
                        painterResource(Res.drawable.delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            },
            content = {
                Surface(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth()
                        .testTag("stopwatch-project-${project.id}")
                        .semantics {
                        this.selected = selected
                        customActions = listOf(
                            CustomAccessibilityAction(editActionLabel) {
                                onEdit()
                                true
                            },
                            CustomAccessibilityAction(deleteActionLabel) {
                                deleting = true
                                true
                            },
                        )
                    },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                project.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            linkedPlannorProjectName?.let { linkedName ->
                                Text(
                                    stringResource(Res.string.plannor_linked_project_label, linkedName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            sharedWorkIndicator?.let { indicator -> SharedWorkSyncLabel(indicator) }
                        }
                        if (selected) Icon(painterResource(Res.drawable.done), contentDescription = null)
                    }
                }
            },
        )
    }
}
