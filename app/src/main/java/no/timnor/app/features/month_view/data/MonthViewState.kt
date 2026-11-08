package no.timnor.app.features.month_view.data

import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.work.Work
import no.timnor.app.core.dates.models.DayModel
import no.timnor.app.core.dates.models.Today
import no.timnor.app.features.root.data.ModifyWork

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
