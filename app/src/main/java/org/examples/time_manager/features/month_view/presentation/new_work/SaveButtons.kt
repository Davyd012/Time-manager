package org.examples.time_manager.features.month_view.presentation.new_work

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.examples.time_manager.R
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.features.month_view.MonthViewModel
import org.examples.time_manager.features.month_view.data.MonthViewIntent.ModifyWork
import org.examples.time_manager.features.month_view.data.MonthViewIntent.WriteRangeWork
import org.examples.time_manager.features.month_view.data.MonthViewIntent.WriteWork
import org.examples.time_manager.features.root.presentation.home.utils.getIntFromTime
import org.examples.time_manager.features.root.presentation.home.utils.localDateTime
import org.examples.time_manager.features.root.presentation.home.utils.localDateTimeWithStartTime
import org.examples.time_manager.ui.theme.deleteIcon
import java.time.LocalDateTime


@Composable
fun SaveButtons(
    work: Work?,
    onDismiss: () -> Unit,
    vm: MonthViewModel,
    selectedProject: Int,
    time: String,
    millisToLocalDate: LocalDateTime,
    startedJob: String?,
    notes: String,
    dates: List<LocalDateTime>,
    updateRange: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    if (updateRange) {
        Button(
            onClick = {
                if (dates.size < 2) return@Button
                onDismiss()
                vm.onIntent(
                    WriteRangeWork(
                        project = selectedProject,
                        dates = dates,
                        notes = notes,
                    )
                )
            },
            colors = ButtonDefaults.elevatedButtonColors()
                .copy(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.edit_period_btn),
                modifier = Modifier.padding(10.dp),
                style = typography.titleMedium.copy(color = colors.onPrimary)
            )
        }
        return
    }

    val startedJobDateTime = localDateTime(millisToLocalDate, time, startedJob)
    Log.d("SaveButtons", "$startedJobDateTime / $millisToLocalDate / $startedJob")
    if (work == null) {
        Button(
            onClick = {
                onDismiss()
                vm.onIntent(
                    WriteWork(
                        project = selectedProject,
                        hours = getIntFromTime(time),
                        date = startedJobDateTime,
                        notes = notes,
                    )
                )
            },
            colors = ButtonDefaults.elevatedButtonColors()
                .copy(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.done_btn),
                modifier = Modifier.padding(10.dp),
                style = typography.titleMedium.copy(color = colors.onPrimary)
            )
        }
        return
    }

    Row(modifier = Modifier.padding(5.dp)) {
        Icon(
            deleteIcon(),
            contentDescription = stringResource(R.string.delete_cd),
            tint = colors.onError,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    onDismiss()
                    vm.onIntent(
                        ModifyWork(work = work, delete = true)
                    )
                }
                .background(colors.error)
                .padding(15.dp),
//                    style = typography.titleMedium.copy(color = colors.onPrimary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            stringResource(R.string.save_changes_btn),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    onDismiss()
                    vm.onIntent(
                        ModifyWork(
                            work = work.copy(
                                date = startedJobDateTime,
                                project = selectedProject,
                                time = getIntFromTime(time),
                                description = notes,
                            )
                        )
                    )
                }
                .background(colors.primary)
                .padding(15.dp),
            textAlign = TextAlign.Center,
            style = typography.titleMedium.copy(color = colors.onPrimary)
        )
    }
}
