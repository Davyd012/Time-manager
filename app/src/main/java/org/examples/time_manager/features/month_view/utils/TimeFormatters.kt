package org.examples.time_manager.features.month_view.utils

import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime


fun localDateTimeWithStartTime(
    millisToLocalDate: LocalDateTime,
    startedJob: String
): LocalDateTime = millisToLocalDate
    .withHour(
        startedJob
            .split(":")
            .first()
            .toInt()
    )
    .withMinute(
        startedJob.split(":")[1].toInt()
    )

fun localDateTime(
    millisToLocalDate: LocalDateTime,
    workingHours: String,
    startedJob: String?,
): LocalDateTime {
    if (startedJob != null) return millisToLocalDate.withHour(
        startedJob
            .split(":")
            .first()
            .toInt()
    )
        .withMinute(
            startedJob.split(":")[1].toInt()
        )

    if (millisToLocalDate.toLocalDate() != LocalDate.now()) return millisToLocalDate

    return workingHours.split(":").let {
        LocalDateTime.now()
            .minusHours(it[0].toInt().toLong())
            .minusMinutes(it[1].toInt().toLong())
    }
}

fun getIntFromTime(it: String): Int {
    return it.split(":").let {
        it[0].toInt() * 3600 + it[1].toInt() * 60
    }
}