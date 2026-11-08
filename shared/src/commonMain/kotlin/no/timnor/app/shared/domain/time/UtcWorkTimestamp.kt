package no.timnor.app.shared.domain.time

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Decodes the Room work timestamp using the existing UTC epoch-seconds contract. */
fun utcLocalDateTimeFromEpochSeconds(epochSeconds: Long): LocalDateTime =
    Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.UTC)

/** Encodes a work civil datetime as UTC epoch seconds, matching the existing Room converter. */
fun utcEpochSecondsFromLocalDateTime(dateTime: LocalDateTime): Long =
    dateTime.toInstant(TimeZone.UTC).epochSeconds
