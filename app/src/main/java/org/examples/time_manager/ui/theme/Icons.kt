package org.examples.time_manager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.examples.time_manager.R

@Composable
fun timerIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_timer_24)
}

@Composable
fun pauseTimerIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_pause_24)
}

@Composable
fun exportIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_import_export_24)
}