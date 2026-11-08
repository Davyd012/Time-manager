package no.timnor.app.core.database

import androidx.room.TypeConverter
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        value?.let(::utcLocalDateTimeFromEpochSeconds)?.toJavaLocalDateTime()

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? =
        date?.toKotlinLocalDateTime()?.let(::utcEpochSecondsFromLocalDateTime)
}
