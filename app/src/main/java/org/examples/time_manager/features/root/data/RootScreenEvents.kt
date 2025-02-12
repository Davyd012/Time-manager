package org.examples.time_manager.features.root.data

import android.content.Context
import android.net.Uri
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import java.time.LocalDateTime

sealed interface RootScreenEvents {
    data class NewProjectEvent(val name: String, val description: String, val project: Project?) :
        RootScreenEvents

    data class SelectDayEvent(val value: Int) : RootScreenEvents
    data class SelectProjectEvent(val value: Int) : RootScreenEvents
    data class UpdateTimerEvent(val type: TimerStates) : RootScreenEvents
    data class WriteWorkEvent(
        val project: Int,
        val hours: Int,
        val date: LocalDateTime,
        val notes: String
    ) :
        RootScreenEvents

    data class ModifyExportProjects(val project: Project) : RootScreenEvents

    data class WriteRangeWorkEvent(
        val project: Int,
        val dates: List<LocalDateTime>,
        val started: LocalDateTime,
        val notes: String
    ) :
        RootScreenEvents

    data class ModifyWorkEvent(val work: Work, val delete: Boolean = false) : RootScreenEvents
    data class CreateExcelDocumentEvent(
        val context: Context,
        val uri: Uri,
        val month: Int? = null
    ) :
        RootScreenEvents

    data class ModifyWorkStateEvent(val selected: Int = -1, val show: Boolean) : RootScreenEvents

    data class ModifyProjectEvent(val project: Project, val delete: Boolean = false) :
        RootScreenEvents
}

sealed interface TimerStates {
    data object StartStopwatch : TimerStates
    data object PauseStopwatch : TimerStates
    data object SaveResult : TimerStates
}