package org.examples.time_manager.features.root.presentation.utils

import android.annotation.SuppressLint


@SuppressLint("DefaultLocale")
fun normalizeTime(seconds: Double): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02.0f:%02.0f", minutes, remainingSeconds)
}