package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue
import no.timnor.app.shared.ui.theme.timnorSpacing

/** Small decorative project marker that uses the established Material 3 theme roles. */
@Composable
fun ProjectIndicator(
    projectId: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(MaterialTheme.timnorSpacing.small)
            .clip(CircleShape)
            .background(projectIndicatorColor(projectId)),
    )
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
