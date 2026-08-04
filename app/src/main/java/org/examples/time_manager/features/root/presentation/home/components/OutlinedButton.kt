package org.examples.time_manager.features.root.presentation.home.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun OutlinedButton(
    value: String,
    text: String,
    icon: ImageVector,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val valueStyle = if (isPrimary) typography.headlineSmall else typography.bodyLarge

    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = {
            Text(text, color = colors.onSurfaceVariant)
        },
        textStyle = valueStyle.copy(color = colors.onSurface),
        leadingIcon = { Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant) },
        shape = MaterialTheme.shapes.medium,
        readOnly = true,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.outlineVariant,
            unfocusedBorderColor = colors.outlineVariant,
            focusedContainerColor = colors.surfaceContainerLow,
            unfocusedContainerColor = colors.surfaceContainerLow,
        ),
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            action()
                        }
                    }
                }
            },
        modifier = modifier.fillMaxWidth()
    )
}
