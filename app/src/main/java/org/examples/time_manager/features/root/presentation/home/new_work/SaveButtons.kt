package org.examples.time_manager.features.root.presentation.home.new_work

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.WriteWorkEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.WriteRangeWorkEvent
import org.examples.time_manager.features.root.presentation.home.utils.getIntFromTime
import org.examples.time_manager.features.root.presentation.home.utils.localDateTime
import org.examples.time_manager.ui.theme.deleteIcon
import org.examples.time_manager.ui.theme.doneIcon
import java.time.LocalDateTime


@Composable
fun SaveButtons(
    work: Work?,
    onDismiss: () -> Unit,
    vm: HomeViewModel,
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
    val primaryButtonShape = RoundedCornerShape(14.dp)
    val primaryButtonColors = ButtonDefaults.buttonColors(
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
    )
    val primaryButtonPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
    val primaryButtonModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)

    if (updateRange) {
        Button(
            onClick = {
                if (dates.size < 2) return@Button
                onDismiss()
                vm.onEvent(
                    WriteRangeWorkEvent(
                        project = selectedProject,
                        dates = dates,
                        notes = notes,
                    )
                )
            },
            colors = primaryButtonColors,
            shape = primaryButtonShape,
            modifier = primaryButtonModifier,
            contentPadding = primaryButtonPadding,
        ) {
            Icon(doneIcon(), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Registrer periode",
                style = typography.titleMedium
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
                vm.onEvent(
                    WriteWorkEvent(
                        project = selectedProject,
                        hours = getIntFromTime(time),
                        date = startedJobDateTime,
                        notes = notes,
                    )
                )
            },
            colors = primaryButtonColors,
            shape = primaryButtonShape,
            modifier = primaryButtonModifier,
            contentPadding = primaryButtonPadding,
        ) {
            Icon(doneIcon(), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Ferdig",
                style = typography.titleMedium
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                onDismiss()
                vm.onEvent(
                    ModifyWorkEvent(work = work, delete = true)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.error,
                contentColor = colors.onError,
            ),
            shape = primaryButtonShape,
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(deleteIcon(), contentDescription = "Fjerne")
        }
        Button(
            onClick = {
                onDismiss()
                vm.onEvent(
                    ModifyWorkEvent(
                        work = work.copy(
                            date = startedJobDateTime,
                            project = selectedProject,
                            time = getIntFromTime(time),
                            description = notes,
                        )
                    )
                )
            },
            colors = primaryButtonColors,
            shape = primaryButtonShape,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            contentPadding = primaryButtonPadding,
        ) {
            Icon(doneIcon(), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lagre endringer", style = typography.titleMedium)
        }
    }
}
