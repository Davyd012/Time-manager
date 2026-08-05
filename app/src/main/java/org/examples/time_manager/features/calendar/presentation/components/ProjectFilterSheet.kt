package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.ui.theme.arrowLeftIcon
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFilterSheet(
    projects: List<Project>,
    selectedProjects: List<Project>,
    projectHours: Map<Int, Double>,
    onApply: (List<Project>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allProjectIds = remember(projects) { projects.map(Project::id).toSet() }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by remember(selectedProjects, allProjectIds) {
        mutableStateOf(selectedProjects.map(Project::id).toSet().takeUnless { it.isEmpty() })
    }
    val visibleProjects = filterProjects(projects, query)
    val visibleIds = remember(visibleProjects) { visibleProjects.map(Project::id).toSet() }
    val allVisibleSelected = visibleProjects.isNotEmpty() && (
        selectedIds == null || visibleIds.all { it in selectedIds.orEmpty() }
    )

    fun applyAndDismiss() {
        onApply(selectedIds?.let { ids -> projects.filter { it.id in ids } } ?: emptyList())
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::applyAndDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
            ) {
                ProjectFilterHeader(
                    onDismiss = ::applyAndDismiss,
                    onDone = ::applyAndDismiss,
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.project_filter_search_hint)) },
                )

                ProjectFilterSelectAllRow(
                    checked = allVisibleSelected,
                    enabled = visibleProjects.isNotEmpty(),
                    onClick = {
                        selectedIds = toggleVisibleProjectSelection(
                            selectedIds = selectedIds,
                            visibleProjectIds = visibleIds,
                            allProjectIds = allProjectIds,
                        )
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = visibleProjects,
                        key = Project::id,
                    ) { project ->
                        ProjectFilterRow(
                            project = project,
                            monthlyHours = projectHours[project.id] ?: 0.0,
                            checked = selectedIds == null || project.id in selectedIds.orEmpty(),
                            onClick = {
                                selectedIds = toggleProjectSelection(
                                    selectedIds = selectedIds,
                                    projectId = project.id,
                                    allProjectIds = allProjectIds,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun ProjectFilterHeader(
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = arrowLeftIcon(),
                contentDescription = stringResource(R.string.project_filter_close_cd),
            )
        }
        Text(
            text = stringResource(R.string.project_filter_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDone) {
            Text(stringResource(R.string.done_btn))
        }
    }
}

@Composable
private fun ProjectFilterSelectAllRow(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.project_filter_select_all),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Checkbox(checked = checked, onCheckedChange = if (enabled) { { onClick() } } else null)
    }
}

@Composable
private fun ProjectFilterRow(
    project: Project,
    monthlyHours: Double,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(projectIndicatorColor(project.id)),
        )
        Text(
            text = project.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.project_hours, formatProjectHours(monthlyHours)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Checkbox(checked = checked, onCheckedChange = { onClick() })
    }
}

@Composable
private fun projectIndicatorColor(projectId: Int): Color {
    val colors = MaterialTheme.colorScheme
    return when (projectId.toLong().absoluteValue % 6L) {
        0L -> colors.primary
        1L -> colors.secondary
        2L -> colors.tertiary
        3L -> colors.primaryContainer
        4L -> colors.secondaryContainer
        else -> colors.tertiaryContainer
    }
}

private fun formatProjectHours(hours: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return formatter.format(hours)
}
