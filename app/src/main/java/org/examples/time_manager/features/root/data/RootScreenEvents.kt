package org.examples.time_manager.features.root.data

import java.time.LocalDateTime

sealed interface RootScreenEvents {
    data class NewProjectEvent(val name: String, val description: String) : RootScreenEvents
    data class SelectDayEvent(val value: Int) : RootScreenEvents
    data class SelectProjectEvent(val value: Int) : RootScreenEvents
    data class UpdateTimerEvent(val type: TimerStates) : RootScreenEvents
    data class WriteWorkEvent(val project: Int, val hours: Int, val date: LocalDateTime) : RootScreenEvents
}

sealed interface TimerStates {
    data object StartStopwatch : TimerStates
    data object PauseStopwatch : TimerStates
    data object SaveResult : TimerStates
}