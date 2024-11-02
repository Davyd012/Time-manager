package org.examples.time_manager.features.root.presentation.home.new_work

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.database.project.Project

@Composable
fun ListOfProjects(
    projects: List<Project>,
    selectedProject: Int,
    selectProject: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val scrollState = rememberScrollState()

    Row(modifier = Modifier.horizontalScroll(state = scrollState)) {
        projects.forEachIndexed { i, it ->
            val paddingModifier = Modifier.padding(
                start = if (i == projects.size) 5.dp else 0.dp,
                top = 5.dp,
                end = 7.dp,
                bottom = 5.dp
            )

            Text(
                text = it.name,
                style = typography.titleMedium.copy(color = if (it.id == selectedProject) colors.onPrimaryContainer else colors.onSecondaryContainer),
                modifier = paddingModifier
                    .clip(
                        RoundedCornerShape(5.dp)
                    )
                    .clickable { selectProject(it.id) }
                    .background(if (it.id == selectedProject) colors.primaryContainer else colors.secondaryContainer)
                    .padding(15.dp),
            )
        }
    }
}