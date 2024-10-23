package org.examples.time_manager.features.root.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun HeaderInformation(
    showNewCategoryModal: () -> Unit
) {
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(10.dp)
            .clip(RoundedCornerShape(5.dp))
//            .clickable { }
            .padding(5.dp)
    ) {
        Spacer(modifier = Modifier.padding(start = 2.dp))
//        Box {
//            Text(
//                "Bedriften Min AS",
//                style = style.bodyLarge.copy(color = colors.onPrimaryContainer)
//            )
//        }
        Spacer(modifier = Modifier.width(5.dp))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { showNewCategoryModal() }) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
//            Spacer(modifier = Modifier.width(10.dp))
    }
}