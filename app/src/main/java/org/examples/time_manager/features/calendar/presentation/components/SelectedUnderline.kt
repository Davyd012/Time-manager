package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SelectedUnderline(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(bottom = 2.dp)
                .width(36.dp)
                .height(2.dp)
                .background(
                    brush =
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.2f), color, color.copy(alpha = 0.2f)),
                        ),
                    shape = RoundedCornerShape(999.dp),
                ),
    )
}
