package no.timnor.app.shared.domain.project

/** Filters a project collection by its display name without changing project identity. */
fun <T> filterProjects(
    projects: List<T>,
    query: String,
    nameOf: (T) -> String,
): List<T> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return projects
    return projects.filter { nameOf(it).contains(normalizedQuery, ignoreCase = true) }
}

/** Aggregates duration seconds by project and returns decimal hours. */
fun <T> projectHoursById(
    works: List<T>,
    projectIdOf: (T) -> Int,
    durationSecondsOf: (T) -> Int,
): Map<Int, Double> = works.groupBy(projectIdOf)
    .mapValues { (_, projectWorks) -> projectWorks.sumOf(durationSecondsOf) / 3600.0 }
