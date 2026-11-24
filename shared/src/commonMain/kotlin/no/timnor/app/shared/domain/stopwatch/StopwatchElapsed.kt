package no.timnor.app.shared.domain.stopwatch

/** Matches the Android project-card rule: lock selection after elapsed time is recorded. */
fun canChangeStopwatchProject(elapsedSeconds: Int): Boolean = elapsedSeconds <= 0

internal const val STOPWATCH_MILLIS_PER_SECOND = 1_000L
internal const val MAX_STOPWATCH_MILLIS = Int.MAX_VALUE * STOPWATCH_MILLIS_PER_SECOND

/** Derives elapsed time from persisted timestamps, including time spent suspended. */
internal fun stopwatchElapsedMillis(accumulatedMillis: Long, startedAtMillis: Long?, nowMillis: Long): Long {
    val accumulated = accumulatedMillis.coerceIn(0, MAX_STOPWATCH_MILLIS)
    val running = if (startedAtMillis != null && startedAtMillis in 0..nowMillis) {
        (nowMillis - startedAtMillis).coerceAtMost(MAX_STOPWATCH_MILLIS)
    } else {
        0L
    }
    return (accumulated + running).coerceAtMost(MAX_STOPWATCH_MILLIS)
}
