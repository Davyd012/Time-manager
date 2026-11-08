package no.timnor.app.features.root.data

import no.timnor.app.core.database.project.Project

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
