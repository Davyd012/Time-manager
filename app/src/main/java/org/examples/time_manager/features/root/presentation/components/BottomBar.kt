package org.examples.time_manager.features.root.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.examples.time_manager.navigation.NavHomeRoutes
import org.examples.time_manager.navigation.PageNavigator
import org.examples.time_manager.ui.theme.timerIcon

@Composable
fun BottomBar(
    navigator: PageNavigator
) {
    var currentRoute by remember { mutableStateOf<NavHomeRoutes>(NavHomeRoutes.Home) }
    val colors = MaterialTheme.colorScheme

    fun isCurrent(route: NavHomeRoutes): Boolean {
        return route == currentRoute
    }

    val bottomBarItems = listOf(
        BottomBarItem(
            NavHomeRoutes.Home,
            Icons.Outlined.Home,
            activeIcon = Icons.Default.Home,
            "Hjem"
        ),
        BottomBarItem(NavHomeRoutes.Timer, timerIcon(), timerIcon(), "Stoppeklokke"),
        BottomBarItem(
            NavHomeRoutes.Settings,
            Icons.Default.Add, Icons.Outlined.Add, "Ny prosjekt"
        )
    )

    NavigationBar(modifier = Modifier.height(75.dp), containerColor = colors.primary) {
        bottomBarItems.forEach { destination ->
            val isSelected = isCurrent(destination.direction)
            NavigationBarItem(
                selected = isSelected,
                onClick = {
//                    if (isCurrentDestOnBackStack) {
//                        navigator.close()
//                        return@NavigationBarItem
//                    }
                    navigator.changePage(destination.direction)
                    currentRoute = destination.direction
                },
                icon = {
                    Icon(
                        if (isSelected) destination.activeIcon else destination.icon,
                        contentDescription = destination.label,
                        tint = if (isSelected) colors.onPrimaryContainer else colors.onPrimary
                    )
                },
                label = { Text(destination.label, color = if (isSelected) colors.onSecondaryContainer else colors.onPrimary) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = colors.primaryContainer)
            )
        }
    }

}

data class BottomBarItem(
    val direction: NavHomeRoutes,
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val label: String
)