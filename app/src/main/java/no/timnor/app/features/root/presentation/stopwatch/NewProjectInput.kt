package no.timnor.app.features.root.presentation.stopwatch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.timnor.app.R
import no.timnor.app.core.database.project.Project
import no.timnor.app.features.root.data.StopwatchIntent
import no.timnor.app.features.root.data.StopwatchIntent.NewProject
import no.timnor.app.features.root.presentation.newProject.InfoTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectInput(
    onDismiss: () -> Unit,
    onIntent: (event: StopwatchIntent) -> Unit,
    project: Project?
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        shape = MaterialTheme.shapes.large,
        dragHandle = null,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
    ) {
        NewProjectSheetContent(
            project = project,
            onIntent = onIntent,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun NewProjectSheetContent(
    project: Project?,
    onIntent: (StopwatchIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography
    var name by rememberSaveable(project?.id) { mutableStateOf(project?.name ?: "") }
    var description by rememberSaveable(project?.id) { mutableStateOf(project?.description ?: "") }

    Column(
        modifier = Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
            Text(stringResource(R.string.new_project_title), style = texts.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(5.dp))
            Text(stringResource(R.string.name_label), style = texts.titleMedium)
            Spacer(modifier = Modifier.height(5.dp))
            InfoTextField(
                name, stringResource(R.string.name_hint), { name = it }, 1,
                modifier = Modifier
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(stringResource(R.string.description_label), style = texts.titleMedium)
            Spacer(modifier = Modifier.height(5.dp))
            InfoTextField(
                description, stringResource(R.string.description_hint), { description = it }, 7,
                modifier = Modifier
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(50.dp))

            Button(
                onClick = {
                    onIntent(NewProject(name = name, description = description, project = project))
                    onDismiss()
                },
                colors = ButtonDefaults.elevatedButtonColors()
                    .copy(containerColor = colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.new_project_btn).takeIf { project == null } ?: stringResource(R.string.save_btn),
                    modifier = Modifier.padding(10.dp),
                    style = texts.titleMedium.copy(color = colors.onPrimary)
                )
            }
        }
}
