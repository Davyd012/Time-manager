package org.examples.time_manager.features.utils


import android.annotation.SuppressLint
import java.util.Locale
import kotlin.math.round


@SuppressLint("DefaultLocale")
fun normalizeTime(seconds: Double): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02.0f:%02.0f", minutes, remainingSeconds)
}

fun formatHoursFromSeconds(seconds: Int): String {
    val hours = seconds / 3600.0
    val roundedHours = String.format(Locale.US, "%.1f", hours).toDouble()
    return if (roundedHours % 1 == 0.0) {
        roundedHours.toInt().toString()
    } else {
        roundedHours.toString()
    }
}