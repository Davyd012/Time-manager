package org.examples.time_manager.features.calendar.presentation.components

import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work

internal fun filterProjects(
    projects: List<Project>,
    query: String,
): List<Project> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return projects
    return projects.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
}

internal fun toggleProjectSelection(
    selectedIds: Set<Int>?,
    projectId: Int,
    allProjectIds: Set<Int>,
): Set<Int>? {
    val next = (selectedIds ?: allProjectIds).toMutableSet()
    if (!next.add(projectId)) next.remove(projectId)
    return next.takeUnless { it.isEmpty() || it == allProjectIds }
}

internal fun toggleVisibleProjectSelection(
    selectedIds: Set<Int>?,
    visibleProjectIds: Set<Int>,
    allProjectIds: Set<Int>,
): Set<Int>? {
    if (visibleProjectIds.isEmpty()) return selectedIds

    val allVisibleSelected = selectedIds == null || visibleProjectIds.all { it in selectedIds }
    val next = (selectedIds ?: allProjectIds).toMutableSet()
    if (allVisibleSelected) {
        next.removeAll(visibleProjectIds)
    } else {
        next.addAll(visibleProjectIds)
    }
    return next.takeUnless { it.isEmpty() || it == allProjectIds }
}

internal fun projectHoursById(works: List<Work>): Map<Int, Double> =
    works.groupBy { it.project }
        .mapValues { (_, projectWorks) -> projectWorks.sumOf { it.time } / 3600.0 }
