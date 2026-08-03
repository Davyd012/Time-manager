package org.examples.time_manager.features.root.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import java.time.YearMonth

data class HomeState(
    val counting: Boolean = false,
    val countVerses: Int = 0,
    val dayPerMonth: List<DayModel> = emptyList(),
    val selectedDay: Int = 0,
    val selectedProject: Int = 1,
    val selectedWork: ModifyWork = ModifyWork(),
    val projects: Flow<List<Project>> = emptyFlow(),
    val workQueries: Flow<List<Work>> = emptyFlow(),
    val today: Today = Today("", 0, "", 0),
    // Calendar state is deliberately separate from the day view state above. A user can
    // browse/filter the calendar without changing the work list currently shown on Home.
    val calendarMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<DayModel> = emptyList(),
    val calendarSelectedProjects: List<Project> = emptyList(),
)

data class ModifyWork(
    val selectedWork: Int = -1,
    val showModal: Boolean = false,
)
