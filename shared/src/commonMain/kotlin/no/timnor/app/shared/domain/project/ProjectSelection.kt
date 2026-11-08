package no.timnor.app.shared.domain.project

/**
 * Toggles one project in a selection where `null` means all projects are selected.
 */
fun toggleProjectSelection(
    selectedIds: Set<Int>?,
    projectId: Int,
    allProjectIds: Set<Int>,
): Set<Int>? {
    val next = (selectedIds ?: allProjectIds).toMutableSet()
    if (!next.add(projectId)) next.remove(projectId)
    return next.takeUnless { it.isEmpty() || it == allProjectIds }
}

/**
 * Toggles the visible projects while preserving the implicit select-all state.
 */
fun toggleVisibleProjectSelection(
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
