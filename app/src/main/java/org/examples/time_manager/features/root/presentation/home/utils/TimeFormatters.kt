package org.examples.time_manager.features.root.presentation.home.utils

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

fun localDateTime(millisToLocalDate: String): LocalDateTime {
    val now = LocalDateTime.now()
    millisToLocalDate.split(":").let {
        return now
            .withHour(now.hour - it[0].toInt())
            .withMinute(now.minute - it[1].toInt())
    }
}

fun getIntFromTime(it: String): Int {
    return it.split(":").let {
        it[0].toInt() * 3600 + it[1].toInt() * 60
    }
}