package no.timnor.app.features.sharedhours

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.timnor.app.R
import no.timnor.app.core.repository.PlannorConnectionFailure
import no.timnor.app.core.repository.PlannorSession
import no.timnor.app.shared.ui.theme.timnorSpacing

enum class PlannorSettingsDestination { Account, Projects, ShareTime, CreateTime, AllocationsReview }

@Composable
fun PlannorConnectionSection(
    viewModel: PlannorConnectionViewModel,
    destination: PlannorSettingsDestination,
    modifier: Modifier = Modifier,
    onConnectRequired: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onCreateTime: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        val session = state.session
        when {
            destination == PlannorSettingsDestination.Account && session == null -> SignInForm(state, viewModel)
            session == null -> {
                Text(stringResource(R.string.plannor_connect_required))
                Button(onClick = onConnectRequired, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.plannor_connection_title))
                }
            }
            destination == PlannorSettingsDestination.Account -> AccountDetails(
                session, state, viewModel::signOut, onOpenProjects, onCreateTime,
            )
            destination == PlannorSettingsDestination.CreateTime -> CreateSharedTimeForm(state, viewModel)
        }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { error ->
            Text(stringResource(error.toStringResource()), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
        }
        if (state.projectError) {
            Text(stringResource(R.string.plannor_error_projects), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = viewModel::refreshProjects, enabled = !state.loading) {
                Text(stringResource(R.string.entry_retry))
            }
        }
    }
}

@Composable
private fun SignInForm(state: PlannorConnectionUiState, viewModel: PlannorConnectionViewModel) {
    Text(stringResource(R.string.plannor_connection_private), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(value = state.identifier, onValueChange = viewModel::setIdentifier,
        label = { Text(stringResource(R.string.plannor_identifier)) }, enabled = !state.loading,
        singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = state.password, onValueChange = viewModel::setPassword,
        label = { Text(stringResource(R.string.plannor_password)) }, enabled = !state.loading,
        singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Button(onClick = viewModel::signIn, enabled = !state.loading && state.identifier.isNotBlank() && state.password.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.plannor_connect)) }
}

@Composable
private fun AccountDetails(session: PlannorSession, state: PlannorConnectionUiState,
    onSignOut: () -> Unit, onOpenProjects: () -> Unit, onCreateTime: () -> Unit) {
    var confirmSignOut by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(MaterialTheme.timnorSpacing.medium), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium)) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(session.username.take(1).uppercase(), modifier = Modifier.padding(MaterialTheme.timnorSpacing.medium),
                    style = MaterialTheme.typography.titleLarge)
            }
            Column {
                Text(stringResource(R.string.plannor_connection_title), style = MaterialTheme.typography.titleMedium)
                Text(session.username, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.account_connected), color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Text(stringResource(R.string.account_local_privacy), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.small))
    OutlinedButton(onClick = onOpenProjects, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.account_projects, state.projects.count { it.selected }))
    }
    TextButton(onClick = onCreateTime, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.plannor_share_time_title)) }
    OutlinedButton(onClick = { confirmSignOut = true }, enabled = !state.loading) { Text(stringResource(R.string.plannor_sign_out)) }
    if (confirmSignOut) {
        AlertDialog(onDismissRequest = { confirmSignOut = false }, title = { Text(stringResource(R.string.plannor_sign_out)) },
            text = { Text(stringResource(R.string.plannor_sign_out_confirmation)) },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text(stringResource(R.string.plannor_sign_out)) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.plannor_import_cancel)) } })
    }
}

@Composable
private fun CreateSharedTimeForm(state: PlannorConnectionUiState, viewModel: PlannorConnectionViewModel) {
    val projects = state.projects.filter { it.selected && !it.archived }
    Text(stringResource(R.string.plannor_share_time_privacy), style = MaterialTheme.typography.bodyMedium)
    if (projects.isEmpty()) Text(stringResource(R.string.entry_no_selected_project), color = MaterialTheme.colorScheme.onSurfaceVariant)
    projects.forEach { project ->
        TextButton(onClick = { viewModel.selectSharedProject(project.projectId) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.sharedProjectId == project.projectId) R.string.plannor_share_target_selected else R.string.plannor_share_target, project.name))
        }
    }
    if (projects.isNotEmpty()) {
        TaskPicker(state, viewModel)
        SharedWorkDateControl(state.sharedWorkDate, viewModel::setSharedWorkDate)
        SharedDurationControl(state.sharedMinutes, viewModel::setSharedMinutes)
        OutlinedTextField(value = state.privateNote, onValueChange = viewModel::setPrivateNote,
            label = { Text(stringResource(R.string.plannor_private_note)) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.sharedNote, onValueChange = viewModel::setSharedNote,
            label = { Text(stringResource(R.string.plannor_shared_note)) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth())
        if (state.pendingDirectCreate) {
            Text(stringResource(R.string.entry_pending), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = viewModel::retryPendingWrites, enabled = !state.loading) { Text(stringResource(R.string.plannor_retry_shared_time)) }
        }
        Button(onClick = viewModel::createSharedTime,
            enabled = !state.loading && !state.pendingDirectCreate && state.sharedMinutes.toLongOrNull()?.let { it > 0 } == true,
            colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.plannor_create_shared_time)) }
    }
    WriteResult(state, viewModel::retryPendingWrites)
}

@Composable
private fun TaskPicker(state: PlannorConnectionUiState, viewModel: PlannorConnectionViewModel) {
    var expanded by remember(state.sharedProjectId) { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        OutlinedButton(onClick = { expanded = true }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text(state.tasks.firstOrNull { it.id == state.sharedTaskId }?.headline ?: stringResource(R.string.plannor_task_optional))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.plannor_task_optional)) }, onClick = { viewModel.selectSharedTask(null); expanded = false })
            state.tasks.forEach { task ->
                DropdownMenuItem(text = { Text(task.headline) }, onClick = { viewModel.selectSharedTask(task.id); expanded = false })
            }
        }
    }
}

private fun PlannorConnectionFailure.toStringResource(): Int = when (this) {
    PlannorConnectionFailure.CREDENTIALS_OR_ACCOUNT -> R.string.plannor_error_credentials
    PlannorConnectionFailure.RATE_LIMITED -> R.string.plannor_error_rate_limited
    PlannorConnectionFailure.SERVER -> R.string.plannor_error_server
    PlannorConnectionFailure.STORAGE -> R.string.plannor_error_storage
    PlannorConnectionFailure.NETWORK, PlannorConnectionFailure.SESSION_EXPIRED -> R.string.plannor_error_network
}
