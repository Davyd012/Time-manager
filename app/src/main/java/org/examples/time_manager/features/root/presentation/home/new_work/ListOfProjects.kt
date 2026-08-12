package org.examples.time_manager.features.root.presentation.home.new_work

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
            val isSelected = project.id in selectedProjects

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    colors.primaryContainer
                } else {
                    colors.primary
                },
                label = "projectContainerColor",
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    colors.onPrimaryContainer
                } else {
                    colors.onSurface
                },
                label = "projectContentColor",
            )

            Surface(
                onClick = { selectProject(project.id) },
                shape = MaterialTheme.shapes.small,
                color = containerColor,
                contentColor = contentColor,
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(
                        width = 1.dp,
                        color = colors.onSurface.copy(alpha = .25f),
                    )
                },
            ) {
                Text(
                    text = project.name,
                    style = typography.titleMedium,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 12.dp,
                    ),
                )
            }
        }
    }
}
