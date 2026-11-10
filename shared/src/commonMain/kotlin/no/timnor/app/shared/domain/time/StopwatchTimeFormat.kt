package no.timnor.app.shared.domain.time

/** Formats elapsed stopwatch time as total minutes and seconds. */
fun formatStopwatchTime(seconds: Int): String {
    val elapsed = seconds.coerceAtLeast(0)
    val minutes = elapsed / 60
    val remainingSeconds = elapsed % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}
