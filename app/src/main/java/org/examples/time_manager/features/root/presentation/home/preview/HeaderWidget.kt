package org.examples.time_manager.features.root.presentation.home.preview

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.examples.time_manager.features.root.HomeState
import org.examples.time_manager.features.root.HomeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HeaderWidget(
    it: PaddingValues,
    colors: ColorScheme,
    state: HomeState,
    listState: LazyListState,
    vm: HomeViewModel
) {
    val texts = MaterialTheme.typography

    Column(
        modifier = Modifier
            .padding(it)
            .clip(RoundedCornerShape(bottomEnd = 30.dp, bottomStart = 30.dp))
            .background(colors.primary)
            .padding(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.padding(start = 2.dp))
            //        Box {
            //            Text(
            //                "Bedriften Min AS",
            //                style = style.bodyLarge.copy(color = colors.onPrimaryContainer)
            //            )
            //        }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                state.today.weekDay,
                style = texts.headlineMedium.copy(color = colors.onPrimary)
            )
            Icon(
                Icons.Default.KeyboardArrowDown, contentDescription = null,
                tint = colors.onPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { }) {
                Icon(
                    Icons.Outlined.AddCircle,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        MonthDaysList(
            listState, state, vm
        )

    }
}