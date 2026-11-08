package no.timnor.app.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.shared.data.persistence.StoredProject

@Composable
internal fun ProjectSelector(
    projects: List<StoredProject>,
    selectedProjectId: Int?,
    onProjectSelected: (Int) -> Unit,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    placeholder: String = "",
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Button(onClick = { onExpandedChange(true) }, enabled = projects.isNotEmpty()) {
            Text(projects.firstOrNull { it.id == selectedProjectId }?.name ?: placeholder)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.name) },
                    onClick = {
                        onProjectSelected(project.id)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}
