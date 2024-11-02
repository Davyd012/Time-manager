package org.examples.time_manager.features.root.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import java.time.LocalDate

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
)

data class ModifyWork(
    val selectedWork: Int = -1,
    val showModal: Boolean = false,
)

data class DayModel(
    val day: String,
    val date: LocalDate,
    val time: Int,
)

data class Today(
    val weekDay: String,
    val day: Int,
    val month: String,
    val year: Int,
)