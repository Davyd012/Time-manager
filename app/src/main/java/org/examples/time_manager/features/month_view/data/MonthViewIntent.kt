package org.examples.time_manager.features.month_view.data

import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import java.time.LocalDateTime

sealed interface MonthViewIntent {
    data class ModifyWork(val work: Work, val delete: Boolean = false) : MonthViewIntent
    data class ModifyWorkState(val selected: Int = -1, val show: Boolean) : MonthViewIntent
    data class SelectDay(val value: Int) : MonthViewIntent
    data class WriteWork(val project: Int, val hours: Int, val date: LocalDateTime, val notes: String) : MonthViewIntent
    data class WriteRangeWork(val project: Int, val dates: List<LocalDateTime>, val notes: String) : MonthViewIntent
    data class NewProject(val name: String, val description: String, val project: Project?) : MonthViewIntent
    data class ModifyProject(val project: Project, val delete: Boolean = false) : MonthViewIntent
    data class ModifyExportProjects(val project: Project) : MonthViewIntent
    data class CreateExcelDocument(val uri: String) : MonthViewIntent
}
