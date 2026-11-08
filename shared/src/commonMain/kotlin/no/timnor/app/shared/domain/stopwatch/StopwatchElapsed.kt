package no.timnor.app.shared.domain.stopwatch

/** Derives a displayed stopwatch value from persisted start/pause instants. */
fun stopwatchElapsedSeconds(
    accumulatedSeconds: Long,
    startedAtEpochSeconds: Long?,
    nowEpochSeconds: Long,
): Int {
    val accumulated = accumulatedSeconds.coerceIn(0, Int.MAX_VALUE.toLong())
    val running = startedAtEpochSeconds?.let { start ->
        (nowEpochSeconds - start).coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong())
    } ?: 0L
    return (accumulated + running).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
