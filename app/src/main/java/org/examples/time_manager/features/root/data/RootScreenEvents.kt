package org.examples.time_manager.features.root.data

import android.content.Context
import android.net.Uri
import java.time.LocalDateTime

sealed interface RootScreenEvents {
    data class NewProjectEvent(val name: String, val description: String) : RootScreenEvents
    data class SelectDayEvent(val value: Int) : RootScreenEvents
    data class SelectProjectEvent(val value: Int) : RootScreenEvents
    data class UpdateTimerEvent(val type: TimerStates) : RootScreenEvents
    data class WriteWorkEvent(val project: Int, val hours: Int, val date: LocalDateTime) :
        RootScreenEvents

    data class CreateExcelDocumentEvent(val context: Context, val uri: Uri) :
        RootScreenEvents
}

sealed interface TimerStates {
    data object StartStopwatch : TimerStates
    data object PauseStopwatch : TimerStates
    data object SaveResult : TimerStates
}