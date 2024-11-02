package org.examples.time_manager.features.root.presentation.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.SelectProjectEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.UpdateTimerEvent
import org.examples.time_manager.features.root.data.TimerStates
import org.examples.time_manager.features.root.presentation.utils.normalizeTime
import org.examples.time_manager.ui.theme.pauseTimerIcon

@Composable
fun Stopwatch(vm: HomeViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val styles = MaterialTheme.typography

    val state = vm.state.collectAsState().value
    val time = vm.timeCount.collectAsState().value
    val projects by state.projects.collectAsState(initial = emptyList())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomEnd = 30.dp, bottomStart = 30.dp))
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                normalizeTime(time),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        projects.forEach {
            Text(
                text = it.name,
                style = styles.titleMedium.copy(color = if (it.id == state.selectedProject) colors.onPrimaryContainer else colors.onSecondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp, horizontal = 15.dp)
                    .clip(
                        RoundedCornerShape(5.dp)
                    )
                    .clickable { vm.onEvent(SelectProjectEvent(value = it.id)) }
                    .background(if (it.id == state.selectedProject) colors.primaryContainer else colors.secondaryContainer)
                    .padding(15.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        val imageVector =
            if (state.counting) pauseTimerIcon() else Icons.Default.PlayArrow
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector, tint = colors.onTertiaryContainer, contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .clickable {
                        if (state.counting) {
                            vm.onEvent(UpdateTimerEvent(TimerStates.PauseStopwatch))
                            return@clickable
                        }
                        vm.onEvent(UpdateTimerEvent(TimerStates.StartStopwatch))
                    }
                    .background(colors.tertiaryContainer)
                    .padding(15.dp)
                    .size(50.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Ferdig",
                style = styles.titleMedium.copy(
                    colors.onSecondary,
                    fontWeight = FontWeight.W700,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.secondary)
                    .clickable {
                        vm.onEvent(UpdateTimerEvent(TimerStates.SaveResult))
                    }
                    .padding(vertical = 15.dp, horizontal = 30.dp),
            )
//                Icon(
//                    Icons.Default.Star, contentDescription = null,
//                    Modifier
//                )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}