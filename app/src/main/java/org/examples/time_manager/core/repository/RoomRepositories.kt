package org.examples.time_manager.core.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.examples.time_manager.core.database.Database
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RoomWorkRepository(private val dao: WorkDao) : WorkRepository {
    override fun observeForDay(date: LocalDate): Flow<List<Work>> {
        val (start, end) = date.boundaries()
        return dao.getAllWorks(start, end)
    }

    override fun observeForMonth(month: YearMonth, projectIds: List<Int>): Flow<List<Work>> {
        val (start, end) = month.atDay(1).boundaries().first to month.atEndOfMonth().boundaries().second
        return if (projectIds.isEmpty()) dao.getAllWorks(start, end)
        else dao.getAllWorksForProjects(start, end, projectIds)
    }

    override suspend fun hoursForDay(date: LocalDate, projectIds: List<Int>): Int = withContext(Dispatchers.IO) {
        val (start, end) = date.boundaries()
        if (projectIds.isEmpty()) dao.getHoursByDay(start, end)
        else dao.getHoursByDayAndProject(start, end, projectIds)
    }

    override suspend fun upsert(work: Work) = dao.upsert(work)
    override suspend fun delete(work: Work) = dao.delete(work)
}

class RoomProjectRepository(private val dao: ProjectDao) : ProjectRepository {
    override fun observeAll(): Flow<List<Project>> = dao.getAllProjects()
    override suspend fun upsert(project: Project) = dao.upsert(project)
    override suspend fun delete(project: Project) = dao.delete(project)
}

class PoiSpreadsheetExporter(private val context: Context) : SpreadsheetExporter {
    override suspend fun export(uri: String, days: List<org.examples.time_manager.core.dates.models.DayModel>) {
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
