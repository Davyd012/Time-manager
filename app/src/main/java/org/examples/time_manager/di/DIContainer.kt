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
import org.examples.time_manager.core.repository.BackupRepository
import org.examples.time_manager.core.repository.RoomBackupRepository
import org.examples.time_manager.core.repository.DataMutationCoordinator
import org.examples.time_manager.core.repository.AppMutationCoordinator
import org.examples.time_manager.core.service.StopwatchGateway

class DIContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: Database by lazy { getDatabase(applicationContext) }
    val mutationCoordinator: DataMutationCoordinator = AppMutationCoordinator.instance
    val workRepository: WorkRepository by lazy { RoomWorkRepository(database.workDao(), mutationCoordinator) }
    val projectRepository: ProjectRepository by lazy { RoomProjectRepository(database.projectDao(), mutationCoordinator) }
    val backupRepository: BackupRepository by lazy {
        RoomBackupRepository(database, applicationContext.contentResolver, mutationCoordinator)
    }
    val spreadsheetExporter: SpreadsheetExporter by lazy { PoiSpreadsheetExporter(applicationContext) }
    val stopwatchRepository: StopwatchRepository by lazy {
        StopwatchGateway(applicationContext, mutationCoordinator).also { it.bind() }
    }
}
