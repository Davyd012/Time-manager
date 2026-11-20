package no.timnor.app.shared.data.persistence

/** Stores the applied project IDs on this device. A missing value represents all projects. */
class CalendarProjectFilterStore(
    private val read: () -> String?,
    private val write: (String?) -> Unit,
) {
    internal fun load(): Set<Int>? {
        val saved = read() ?: return null
        if (saved.isEmpty()) return emptySet()
        return saved.split(',').map { it.toIntOrNull() ?: return null }.toSet()
    }

    internal fun save(projectIds: Set<Int>?) = write(projectIds?.sorted()?.joinToString(","))
}
