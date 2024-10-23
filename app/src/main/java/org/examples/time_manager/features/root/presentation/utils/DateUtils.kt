package org.examples.time_manager.features.root.presentation.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun convertMillisToLocalDate(millis: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(millis, 0, ZoneOffset.UTC)
        // Interpret the milliseconds as the start of the day in UTC, then convert to Los Angeles time
//        val utcDateAtStartOfDay = Instant
//            .ofEpochMilli(millis)
//            .atZone(ZoneOffset.UTC)
//            .toLocalDate()
//        println("UTC Date at Start of Day: $utcDateAtStartOfDay") // Debugging UTC date
//
//        // Convert to the same instant in Local time zone
//        val localDate = utcDateAtStartOfDay.atStartOfDay(ZoneId.systemDefault())
//        println("Local Date: $localDate") // Debugging local date
//
//        return localDate

    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun dateToString(date: ZonedDateTime?): String {
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM, yyyy", Locale.getDefault())
        if (date != null) {
            return date.format(dateFormatter)
        }
        return "EEEE, dd MMMM, yyyy"
    }
}