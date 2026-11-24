package no.timnor.app.core.service

internal data class StopwatchSession(
    val accumulatedMillis: Long = 0,
    val startedAtMillis: Long? = null,
) {
    fun elapsedMillis(nowMillis: Long): Long {
        val runningMillis = startedAtMillis?.let { (nowMillis - it).coerceIn(0, MAX_ELAPSED_MILLIS) } ?: 0
        return (accumulatedMillis.coerceIn(0, MAX_ELAPSED_MILLIS) + runningMillis).coerceAtMost(MAX_ELAPSED_MILLIS)
    }

    fun start(nowMillis: Long): StopwatchSession =
        if (startedAtMillis == null) copy(startedAtMillis = nowMillis) else this

    fun pause(nowMillis: Long): StopwatchSession = StopwatchSession(elapsedMillis(nowMillis))
}

internal const val MILLIS_PER_SECOND = 1_000L
private const val MAX_ELAPSED_MILLIS = Int.MAX_VALUE * MILLIS_PER_SECOND
