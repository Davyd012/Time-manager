package no.timnor.app.core.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import no.timnor.app.core.database.Database
import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.project.ProjectDao
import no.timnor.app.core.database.work.Work
import no.timnor.app.core.database.work.WorkDao
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RoomWorkRepository(private val store: TimnorPersistence, private val coordinator: DataMutationCoordinator) : WorkRepository {
    override fun observeForDay(date: LocalDate): Flow<List<Work>> {
        val (start, end) = date.boundaries()
        return store.observeWorks(start, end).map { works -> works.map(StoredWork::toWork) }
    }

    override fun observeForMonth(month: YearMonth, projectIds: List<Int>): Flow<List<Work>> {
        val (start, end) = month.atDay(1).boundaries().first to month.atEndOfMonth().boundaries().second
        return store.observeWorks(start, end, projectIds).map { works -> works.map(StoredWork::toWork) }
    }

    override suspend fun hoursForDay(date: LocalDate, projectIds: List<Int>): Int = withContext(Dispatchers.IO) {
        val (start, end) = date.boundaries()
        store.hoursForRange(start, end, projectIds)
    }

    override suspend fun upsert(work: Work) = coordinator.mutate { store.upsertWork(work.toStored()) }
    override suspend fun delete(work: Work) = coordinator.mutate { store.deleteWork(work.toStored()) }
}

class RoomProjectRepository(private val store: TimnorPersistence, private val coordinator: DataMutationCoordinator) : ProjectRepository {
    override fun observeAll(): Flow<List<Project>> = store.observeProjects().map { projects -> projects.map(StoredProject::toProject) }
    override suspend fun upsert(project: Project) = coordinator.mutate { store.upsertProject(project.toStored()) }
    override suspend fun delete(project: Project) = coordinator.mutate { store.deleteProject(project.toStored()) }
}

private fun StoredWork.toWork() = Work(
    description = description,
    date = utcLocalDateTimeFromEpochSeconds(dateEpochSeconds).toJavaLocalDateTime(),
    project = project,
    task = task,
    time = time,
    id = id,
)

private fun Work.toStored() = StoredWork(
    description = description,
    dateEpochSeconds = utcEpochSecondsFromLocalDateTime(date.toKotlinLocalDateTime()),
    project = project,
    task = task,
    time = time,
    id = id,
)

private fun StoredProject.toProject() = Project(description, name, id, hourlyRateOre)

private fun Project.toStored() = StoredProject(description, name, id, hourlyRateOre)

class PoiSpreadsheetExporter(private val context: Context) : SpreadsheetExporter {
    override suspend fun export(uri: String, days: List<no.timnor.app.core.dates.models.DayModel>) {
        withContext(Dispatchers.IO) {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Timer")
            val rightAlignedStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.RIGHT
            }
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            days.forEachIndexed { index, day ->
                val row = sheet.createRow(index)
                row.createCell(0).setCellValue(day.date.format(formatter))
                row.createCell(1).apply { cellStyle = rightAlignedStyle }
                    .setCellValue(day.time / 3600.0)
            }
            context.contentResolver.openOutputStream(android.net.Uri.parse(uri))?.use { output ->
                workbook.write(output)
            }
            workbook.close()
        }
    }
}

private fun LocalDate.boundaries(): Pair<Long, Long> =
    atStartOfDay(ZoneOffset.UTC).toEpochSecond() to
        atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC)
