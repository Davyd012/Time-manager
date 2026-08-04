package org.examples.time_manager.di

import android.content.Context
import org.examples.time_manager.core.database.Database
import org.examples.time_manager.core.database.getDatabase
import org.examples.time_manager.core.repository.PoiSpreadsheetExporter
import org.examples.time_manager.core.repository.ProjectRepository
import org.examples.time_manager.core.repository.RoomProjectRepository
import org.examples.time_manager.core.repository.RoomWorkRepository
import org.examples.time_manager.core.repository.SpreadsheetExporter
import org.examples.time_manager.core.repository.StopwatchRepository
import org.examples.time_manager.core.repository.WorkRepository
import org.examples.time_manager.core.service.StopwatchGateway

class DIContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: Database by lazy { getDatabase(applicationContext) }
    val workRepository: WorkRepository by lazy { RoomWorkRepository(database.workDao()) }
    val projectRepository: ProjectRepository by lazy { RoomProjectRepository(database.projectDao()) }
    val spreadsheetExporter: SpreadsheetExporter by lazy { PoiSpreadsheetExporter(applicationContext) }
    val stopwatchRepository: StopwatchRepository by lazy {
        StopwatchGateway(applicationContext).also { it.bind() }
    }
}
