package org.examples.time_manager.features.month_view.data

import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.features.root.data.ModifyWork

data class MonthViewUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val messageResId: Int? = null,
    val month: String = "",
    val dayPerMonth: List<DayModel> = emptyList(),
    val selectedDay: Int = 1,
    val selectedProject: Int = 1,
    val selectedWork: ModifyWork = ModifyWork(),
    val projects: List<Project> = emptyList(),
    val workQueries: List<Work> = emptyList(),
    val today: Today = Today("", 1, "", 0),
)

typealias MonthViewState = MonthViewUiState
