package no.timnor.app.core.repository

import kotlinx.coroutines.flow.Flow
import no.timnor.app.core.database.work.Work
import java.time.LocalDate
import java.time.YearMonth

interface WorkRepository {
    fun observeForDay(date: LocalDate): Flow<List<Work>>
    fun observeForMonth(month: YearMonth, projectIds: List<Int> = emptyList()): Flow<List<Work>>
    suspend fun hoursForDay(date: LocalDate, projectIds: List<Int> = emptyList()): Int
    suspend fun upsert(work: Work)
    suspend fun delete(work: Work)
}
