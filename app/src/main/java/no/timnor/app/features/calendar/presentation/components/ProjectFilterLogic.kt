package no.timnor.app.features.calendar.presentation.components

import no.timnor.app.core.database.work.Work
import no.timnor.app.core.database.project.Project
import no.timnor.app.shared.domain.project.filterProjects as filterSharedProjects
import no.timnor.app.shared.domain.project.projectHoursById as summarizeSharedProjectHours

internal fun filterProjects(
    projects: List<Project>,
    query: String,
): List<Project> {
    return filterSharedProjects(projects, query, Project::name)
}

internal fun projectHoursById(works: List<Work>): Map<Int, Double> =
    summarizeSharedProjectHours(works, Work::project, Work::time)
