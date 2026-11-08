package no.timnor.app.features.root.presentation.components

import androidx.compose.ui.graphics.vector.ImageVector
import no.timnor.app.navigation.AppRoute
import no.timnor.app.navigation.TopLevelRoute

data class BottomBarItem(
    val direction: TopLevelRoute,
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val label: String
)