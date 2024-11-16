package org.examples.time_manager.features.calendar.utils

import java.util.Locale

fun formatHoursFromSeconds(seconds: Int): String {
    val hours = seconds / 3600.0
    val roundedHours = String.format(Locale.US, "%.1f", hours).toDouble()
    return if (roundedHours % 1 == 0.0) {
        roundedHours.toInt().toString()
    } else {
        roundedHours.toString()
    }
}