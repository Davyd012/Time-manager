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

@Composable
fun watchIcon(filled: Boolean? = true): ImageVector {
    return if (filled == true)
        ImageVector.vectorResource(id = R.drawable.baseline_access_time_filled_24)
    else
        ImageVector.vectorResource(id = R.drawable.baseline_access_time_24)
}

@Composable
fun homeIcon(filled: Boolean? = true): ImageVector {
    return if (filled == true)
        ImageVector.vectorResource(id = R.drawable.baseline_home_filled_24)
    else
        ImageVector.vectorResource(id = R.drawable.outline_home_24)
}

@Composable
fun visibilityIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_visibility_24)
}

@Composable
fun arrowLeftIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.outline_keyboard_arrow_left_24)
}

@Composable
fun arrowRightIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.outline_keyboard_arrow_right_24)
}

@Composable
fun arrowDownIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.outline_keyboard_arrow_down_24)
}

@Composable
fun closeIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_close_24)
}

@Composable
fun playIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_play_arrow_24)
}

@Composable
fun addIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_add_circle_24)
}

@Composable
fun dateRangeIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_date_range_24)
}

@Composable
fun deleteIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_delete_24)
}

@Composable
fun editIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_edit_24)
}

@Composable
fun doneIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_done_24)
}

@Composable
fun shareIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_share_24)
}

@Composable
fun workIcon(): ImageVector {
    return ImageVector.vectorResource(id = R.drawable.baseline_work_outline_24)
}