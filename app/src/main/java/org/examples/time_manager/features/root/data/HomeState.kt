package org.examples.time_manager.features.root.data

import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import java.time.YearMonth

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val messageResId: Int? = null,
    val counting: Boolean = false,
    val elapsedSeconds: Int = 0,
    val dayPerMonth: List<DayModel> = emptyList(),
    val selectedDay: Int = 1,
    val selectedProject: Int = 1,
    val selectedWork: ModifyWork = ModifyWork(),
    val projects: List<Project> = emptyList(),
    val workQueries: List<Work> = emptyList(),
    val today: Today = Today("", 1, "", 0),
    val calendarMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<DayModel> = emptyList(),
    val calendarSelectedProjects: List<Project> = emptyList(),
    val calendarProjectHours: Map<Int, Double> = emptyMap(),
)

typealias HomeState = HomeUiState

data class StopwatchUiState(
    val isLoading: Boolean = true,
    val messageResId: Int? = null,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val selectedProject: Int = 1,
    val projects: List<Project> = emptyList(),
)

data class ModifyWork(
    val selectedWork: Int = -1,
    val showModal: Boolean = false,
)
