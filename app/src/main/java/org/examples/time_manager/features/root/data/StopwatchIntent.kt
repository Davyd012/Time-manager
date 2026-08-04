package org.examples.time_manager.features.root.data

import org.examples.time_manager.core.database.project.Project

sealed interface StopwatchIntent {
    data class NewProject(val name: String, val description: String, val project: Project?) : StopwatchIntent
    data class ModifyProject(val project: Project, val delete: Boolean = false) : StopwatchIntent
    data class SelectProject(val value: Int) : StopwatchIntent
    data class UpdateTimer(val type: TimerStates) : StopwatchIntent
}

sealed interface TimerStates {
    data object StartStopwatch : TimerStates
    data object PauseStopwatch : TimerStates
    data object SaveResult : TimerStates
}
