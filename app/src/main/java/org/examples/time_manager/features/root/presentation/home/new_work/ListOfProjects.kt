package org.examples.time_manager.features.root.presentation.home.new_work

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.database.project.Project

@Composable
fun ListOfProjects(
    projects: List<Project>,
    selectedProjects: List<Int>,
    selectProject: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .horizontalScroll(state = scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        projects.forEach { project ->
            val isSelected = selectedProjects.contains(project.id)
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.surfaceVariant
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant
            )

            Text(
                text = project.name,
                style = typography.titleMedium.copy(color = contentColor),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { selectProject(project.id) }
                    .background(backgroundColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
