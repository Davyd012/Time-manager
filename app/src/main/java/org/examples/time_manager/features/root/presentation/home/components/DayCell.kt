package org.examples.time_manager.features.root.presentation.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DayCell(
    modifier: Modifier = Modifier,
    dayLabel: String,              // it.day.substring(0, 3)
    dayOfMonth: Int,               // it.date.dayOfMonth
    hoursText: String,             // formatHoursFromSeconds(it.time)
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: ColorScheme,
    texts: Typography,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Farger (smooth)
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer else colors.primary,
        animationSpec = tween(durationMillis = 160),
        label = "bgColor"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) colors.onPrimaryContainer else colors.onPrimary,
        animationSpec = tween(durationMillis = 160),
        label = "contentColor"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) colors.onPrimaryContainer.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "borderColor"
    )

    // “Pop” ved valg + “shrink” ved press
    val targetScale = when {
        pressed -> 0.96f
        isSelected -> 1.04f
        else -> 1.0f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Løft (elevation) + litt rundere når valgt
    val radius by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 10.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "radius"
    )
    val elevation by animateDpAsState(
        targetValue = when {
            pressed -> 1.dp
            isSelected -> 8.dp
            else -> 3.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "elevation"
    )

    val shape = RoundedCornerShape(radius)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 3.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = dayLabel,
            color = contentColor,
            style = texts.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = dayOfMonth.toString(),
            style = texts.titleMedium.copy(color = contentColor),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = hoursText,
            color = contentColor,
            style = texts.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
