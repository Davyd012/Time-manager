package org.examples.time_manager.features.calendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today

data class CalendarState(
    val counting: Boolean = false,
    val countVerses: Int = 0,
    val dayPerMonth: List<DayModel> = emptyList(),
    val selectedDay: Int = 0,
    val selectedProjects: List<Project> = emptyList(),
    val monthDifference: Int = 0,
    val selectedWork: ModifyWork = ModifyWork(),
    val projects: Flow<List<Project>> = emptyFlow(),
    val workQueries: Flow<List<Work>> = emptyFlow(),
    val today: Today = Today("", 0, "", 0),
)

data class ModifyWork(
    val selectedWork: Int = -1,
    val showModal: Boolean = false,
)