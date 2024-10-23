package org.examples.time_manager.features.root.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RowButtonItem(
    extracted: () -> Unit,
    imageVector: ImageVector,
    title: String,
    description: String
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
            .clip(
                RoundedCornerShape(20.dp)
            )
            .clickable { extracted() }
            .background(colors.primaryContainer)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            imageVector,
            contentDescription = null,
            Modifier.size(35.dp),
            tint = colors.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                title,
                style = typography.titleLarge.copy(
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                description,
                style = typography.bodyMedium.copy(color = colors.onPrimaryContainer.copy(alpha = 0.6f))
            )
        }
    }
}