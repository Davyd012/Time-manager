package org.examples.time_manager.features.root.presentation.components

import androidx.compose.ui.graphics.vector.ImageVector
import org.examples.time_manager.navigation.AppRoute
import org.examples.time_manager.navigation.TopLevelRoute

data class BottomBarItem(
    val direction: TopLevelRoute,
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val label: String
)