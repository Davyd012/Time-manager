package org.examples.time_manager.features.root.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StartButton(text: String, function: () -> Unit = { }) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    Text(
        text,
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (isNumericToX(text)) Modifier else Modifier.clickable { function() })
            .background(colors.secondary)
            .padding(10.dp),
        style = typography.titleLarge.copy(
            color = colors.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        ),
        textAlign = TextAlign.Center
    )
}

fun isNumericToX(toCheck: String): Boolean {
    return toCheck.toDoubleOrNull() != null
}