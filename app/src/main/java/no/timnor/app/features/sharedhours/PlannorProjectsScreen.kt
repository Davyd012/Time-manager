package no.timnor.app.features.sharedhours

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.timnor.app.R
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
fun PlannorProjectsScreen(viewModel: PlannorConnectionViewModel, onConnectRequired: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(state.projectListIndex, state.projectListOffset)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> viewModel.rememberProjectScroll(index, offset) }
    }
    var detailsId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedIds = state.projectDraftSelection ?: state.projects.filter { it.selected }.map { it.projectId }.toSet()
    Column(modifier.imePadding()) {
        if (state.session == null) {
            Text(stringResource(R.string.plannor_connect_required))
            Button(onClick = onConnectRequired) { Text(stringResource(R.string.plannor_connection_title)) }
            return@Column
        }
        val projects = state.projects.filter { state.projectSearch.isBlank() || it.name.contains(state.projectSearch.trim(), ignoreCase = true) }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = listState) {
            item(key = "project-filter") {
                Column {
        Text(stringResource(R.string.project_choose_explanation), style = MaterialTheme.typography.bodyMedium)
        val count = selectedIds.size
        Text(pluralStringResource(R.plurals.settings_selected_projects, count, count),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.small))
        OutlinedTextField(value = state.projectSearch, onValueChange = viewModel::setProjectSearch,
            label = { Text(stringResource(R.string.plannor_search_projects)) },
            leadingIcon = { Icon(painterResource(R.drawable.baseline_search_24), contentDescription = null) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::refreshProjects, enabled = !state.loading) {
                Text(stringResource(if (state.projectError) R.string.entry_retry else R.string.plannor_refresh_projects))
            }
            if (state.loading) CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.timnorSpacing.large))
        }
        if (state.projectError) {
            Text(stringResource(R.string.plannor_error_projects), color = MaterialTheme.colorScheme.error)
        }
        if (state.projectSaveError) {
            Text(stringResource(R.string.project_save_error), color = MaterialTheme.colorScheme.error)
        }
        if (projects.isEmpty() && !state.loading && !state.projectError) {
            Text(stringResource(if (state.projectSearch.isBlank()) R.string.plannor_no_projects else R.string.plannor_no_project_search_results))
        }
                }
            }
            items(projects, key = { it.projectId }) { project ->
                ProjectRow(project.copy(selected = project.projectId in selectedIds), state.loading,
                    onToggle = { viewModel.setProjectDraft(project.projectId, it) },
                    onDetails = { detailsId = project.projectId })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (state.projectDraftSelection != null) {
            Button(onClick = viewModel::saveProjectDraft, enabled = !state.loading,
                colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth()
                .padding(vertical = MaterialTheme.timnorSpacing.small)) {
                Text(stringResource(R.string.project_save_selection, selectedIds.size))
            }
        } else {
            Text(stringResource(R.string.project_selection_saved), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.small))
        }
    }
    state.projects.firstOrNull { it.projectId == detailsId }?.let { project ->
        ProjectLinkSheet(project, state, viewModel, onDismiss = { detailsId = null })
    }
}

@Composable
internal fun ProjectRow(project: PlannorProjectSelection, loading: Boolean, onToggle: (Boolean) -> Unit, onDetails: () -> Unit) {
    val secondaryContentColor = if (project.selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (project.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            val description = stringResource(R.string.plannor_project_accessibility, project.name,
                project.contextLabel?.takeIf(String::isNotBlank) ?: project.organizationName?.takeIf(String::isNotBlank)
                ?: stringResource(R.string.plannor_project_context_unknown))
            Checkbox(checked = project.selected, onCheckedChange = onToggle, enabled = project.available && !project.archived && !loading,
                modifier = Modifier.semantics { contentDescription = description })
            Icon(painterResource(R.drawable.baseline_work_outline_24), contentDescription = null,
                modifier = Modifier.padding(end = MaterialTheme.timnorSpacing.small), tint = secondaryContentColor)
            Column(Modifier.weight(1f).clickable(onClick = onDetails).padding(vertical = MaterialTheme.timnorSpacing.medium)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall)
                Text(project.contextLabel?.takeIf(String::isNotBlank) ?: project.organizationName?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.plannor_project_context_unknown), style = MaterialTheme.typography.bodySmall,
                    color = secondaryContentColor)
                if (project.linkedLocalProjectId != null) Text(stringResource(R.string.project_link_active),
                    style = MaterialTheme.typography.labelMedium, color = secondaryContentColor)
                if (project.archived) Text(stringResource(R.string.archived_project_label),
                    style = MaterialTheme.typography.bodySmall, color = secondaryContentColor)
            }
            TextButton(onClick = onDetails) { Text(stringResource(R.string.project_details)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectLinkSheet(project: PlannorProjectSelection, state: PlannorConnectionUiState,
    viewModel: PlannorConnectionViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = MaterialTheme.timnorSpacing.medium)) {
            Text(project.name, style = MaterialTheme.typography.titleLarge)
            val linked = state.personalProjects.firstOrNull { it.id == project.linkedLocalProjectId }
            Text(linked?.let { stringResource(R.string.plannor_personal_project_linked, it.name) }
                ?: stringResource(R.string.project_not_linked), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.small))
            if (linked == null && !project.archived) {
                TextButton(onClick = { viewModel.addProjectToTimnor(project.projectId); onDismiss() }, enabled = !state.loading) {
                    Text(stringResource(R.string.plannor_add_project_to_timnor))
                }
            }
            if (linked != null) {
                TextButton(onClick = { viewModel.linkProject(project.projectId, null); onDismiss() }, enabled = !state.loading) {
                    Text(stringResource(R.string.plannor_unlink_personal_project))
                }
            }
            Text(stringResource(R.string.project_link_choose), style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
                items(state.personalProjects, key = { it.id }) { local ->
                    ListItem(headlineContent = { Text(local.name) }, modifier = Modifier.clickable(enabled = !state.loading) {
                        viewModel.linkProject(project.projectId, local.id); onDismiss()
                    })
                }
            }
        }
    }
}
