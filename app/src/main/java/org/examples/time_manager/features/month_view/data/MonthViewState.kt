package org.examples.time_manager.features.month_view.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.features.root.data.ModifyWork

data class MonthViewState(
    val month: String = "",

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
