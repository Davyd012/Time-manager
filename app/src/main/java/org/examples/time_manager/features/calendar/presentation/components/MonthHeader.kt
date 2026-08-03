package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.examples.time_manager.App
import org.examples.time_manager.R
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.dateRangeIcon

@Composable
fun MonthHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    colors: MonthViewColors = MonthViewColors.defaults(),
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.background,
    ) {
        Row(
            modifier = Modifier
                .padding(top = App.statusBarHeight)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = arrowLeftIcon(),
                    contentDescription = stringResource(R.string.back_cd),
                    tint = colors.text,
                )
            }

            Text(
                text = stringResource(R.string.calendar_title),
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )

            Icon(
                imageVector = dateRangeIcon(),
                contentDescription = stringResource(R.string.calendar_title),
                tint = colors.accent,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(28.dp),
            )
        }
    }
}
