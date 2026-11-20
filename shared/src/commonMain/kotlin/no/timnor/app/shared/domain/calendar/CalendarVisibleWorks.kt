package no.timnor.app.shared.domain.calendar

import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

internal fun calendarVisibleWorks(
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
): List<StoredWork> {
    if (selectedProjectIds == null) return works
    val validProjectIds = projects.mapTo(mutableSetOf(), StoredProject::id)
    return works.filter { it.project in selectedProjectIds && it.project in validProjectIds }
}
