package org.examples.time_manager.core.repository

import org.examples.time_manager.core.dates.models.DayModel

interface SpreadsheetExporter {
    suspend fun export(uri: String, days: List<DayModel>)
}
