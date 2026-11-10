package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
internal fun ProjectSelector(
    projects: List<StoredProject>,
    selectedProjectId: Int?,
    onProjectSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        projects.forEach { project ->
            FilterChip(
                selected = project.id == selectedProjectId,
                onClick = { onProjectSelected(project.id) },
                label = {
                    Text(
                        project.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = MaterialTheme.timnorSpacing.extraLarge * 6),
                    )
                },
            )
        }
    }
}
