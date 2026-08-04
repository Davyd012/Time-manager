package org.examples.time_manager.features.root.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.examples.time_manager.R
import org.examples.time_manager.navigation.AppRoute
import org.examples.time_manager.navigation.TopLevelRoute
import org.examples.time_manager.ui.theme.homeIcon
import org.examples.time_manager.ui.theme.watchIcon

@Composable
fun BottomBar(
    items: List<BottomBarItem>,
    isCurrent: (TopLevelRoute) -> Boolean,
    onSelect: (TopLevelRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.primary,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()

                // Holder innholdet over gesture-hintet/systemnavigasjonen.
                // Surface-bakgrunnen dekker fortsatt området bak systembaren.
                .navigationBarsPadding()

                // Minimum, ikke fast høyde.
                .heightIn(min = 72.dp)

                // Luft rundt selve navigasjonsinnholdet.
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                BottomBarItemView(
                    item = item,
                    selected = isCurrent(item.direction),
                    onClick = {
                        onSelect(item.direction)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}