package org.examples.time_manager.features.root.data

import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import java.time.LocalDateTime
import java.time.YearMonth

sealed interface HomeIntent {
    data class NewProject(val name: String, val description: String, val project: Project?) : HomeIntent
    data class SelectDay(val value: Int) : HomeIntent
    data class WriteWork(val project: Int, val hours: Int, val date: LocalDateTime, val notes: String) : HomeIntent
    data class ModifyExportProjects(val project: Project) : HomeIntent
    data class WriteRangeWork(val project: Int, val dates: List<LocalDateTime>, val notes: String) : HomeIntent
    data class ModifyWork(val work: Work, val delete: Boolean = false) : HomeIntent
    data class CreateExcelDocument(val uri: String, val month: Int? = null) : HomeIntent
    data class ModifyWorkState(val selected: Int = -1, val show: Boolean) : HomeIntent
    data class ModifyProject(val project: Project, val delete: Boolean = false) : HomeIntent
    data class ChangeCalendarMonth(val month: YearMonth) : HomeIntent
    data class ToggleCalendarProject(val project: Project) : HomeIntent
    data object ClearCalendarProjects : HomeIntent
}
