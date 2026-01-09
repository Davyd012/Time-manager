package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.App
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.arrowRightIcon
import org.examples.time_manager.ui.theme.visibilityIcon

@Composable
fun MonthHeader(
    title: String,
    onClose: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewMonth: () -> Unit,
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val gradient =
        Brush.linearGradient(
            colors = listOf(Color(0xFF1C1C1E), Color(0xFF202024)),
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier =
                Modifier
                    .background(gradient, shape)
                    .padding(top = App.statusBarHeight)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = arrowLeftIcon(),
                    contentDescription = "Back",
                    tint = colors.text.copy(alpha = 0.85f),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = arrowLeftIcon(),
                        contentDescription = "Previous month",
                        tint = colors.text.copy(alpha = 0.75f),
                    )
                }
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(160.dp),
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = arrowRightIcon(),
                        contentDescription = "Next month",
                        tint = colors.text.copy(alpha = 0.75f),
                    )
                }
            }
            IconButton(
                onClick = onViewMonth,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = visibilityIcon(),
                    contentDescription = "View month",
                    tint = colors.text.copy(alpha = 0.85f),
                )
            }
        }
    }
}
