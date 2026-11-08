package no.timnor.app.core.repository

import no.timnor.app.core.dates.models.DayModel

interface SpreadsheetExporter {
    suspend fun export(uri: String, days: List<DayModel>)
}
