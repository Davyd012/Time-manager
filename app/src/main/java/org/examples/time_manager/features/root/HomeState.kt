package org.examples.time_manager.features.root

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work

data class HomeState(
    val counting: Boolean = false,
    val countVerses: Int = 0,
    val dayPerMonth: List<DayModel> = emptyList(),
    val selectedDay: Int = 0,
    val selectedProject: Int = 0,
    val projects: Flow<List<Project>> = emptyFlow(),
    val workQueries: Flow<List<Work>> = emptyFlow(),
    val today: Today = Today("", 0, "", 0),
)

data class DayModel(
    val day: String,
    val date: Int,
    val hours: Int,
)

data class Today(
    val weekDay: String,
    val day: Int,
    val month: String,
    val year: Int,
)