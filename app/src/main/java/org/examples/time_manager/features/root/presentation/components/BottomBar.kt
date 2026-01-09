package org.examples.time_manager.features.root.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.examples.time_manager.navigation.HomeRoute
import org.examples.time_manager.navigation.HomeTab
import org.examples.time_manager.navigation.PageNavigator
import org.examples.time_manager.navigation.StopwatchTab
import org.examples.time_manager.ui.theme.homeIcon
import org.examples.time_manager.ui.theme.watchIcon

@Composable
fun BottomBar(
    navigator: PageNavigator,
    currentRoute: HomeRoute,
) {
    val colors = MaterialTheme.colorScheme

    fun isCurrent(route: HomeRoute) = route == currentRoute

    val items = listOf(
        BottomBarItem(
            direction = HomeTab,
            icon = homeIcon(filled = false),
            activeIcon = homeIcon(),
            label = "Hjem"
        ),
        BottomBarItem(
            direction = StopwatchTab,
            icon = watchIcon(filled = false),
            activeIcon = watchIcon(),
            label = "Stoppeklokke"
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = colors.primary,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BottomBarItemView(
                    item = item,
                    selected = isCurrent(item.direction),
                    onClick = {
                        navigator.changePage(item.direction)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BottomBarItemView(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val contentColor =
        if (selected) colors.onSurfaceVariant
        else colors.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = null
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Top indicator
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(45.dp)
                .background(
                    color = if (selected) contentColor else Color.Transparent,
                    shape = RoundedCornerShape(1.dp)
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Icon(
            imageVector = if (selected) item.activeIcon else item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.label,
            style = typography.labelMedium,
            color = contentColor
        )
    }
}


data class BottomBarItem(
    val direction: HomeRoute,
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val label: String
)
