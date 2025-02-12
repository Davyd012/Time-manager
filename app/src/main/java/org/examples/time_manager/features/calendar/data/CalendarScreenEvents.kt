package org.examples.time_manager.features.calendar.data

import org.examples.time_manager.core.database.project.Project

sealed interface CalendarScreenEvents {
    data class UpdateMonth(val month: Int) : CalendarScreenEvents
    data class UpdateSelectedProjects(val project: Project) : CalendarScreenEvents
}