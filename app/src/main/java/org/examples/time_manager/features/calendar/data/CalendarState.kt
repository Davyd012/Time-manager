package org.examples.time_manager.features.calendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import java.time.LocalDate

data class CalendarState(
    val dayPerMonth: List<DayModel> = emptyList(),
    val selectedProjects: List<Project> = emptyList(),
    val monthDifference: Int = 0,
    val projects: Flow<List<Project>> = emptyFlow(),
    val currentDate: LocalDate = LocalDate.now(),
)