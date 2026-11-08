package no.timnor.app.di

import android.content.Context
import no.timnor.app.core.database.Database
import no.timnor.app.core.database.getDatabase
import no.timnor.app.core.repository.PoiSpreadsheetExporter
import no.timnor.app.core.repository.ProjectRepository
import no.timnor.app.core.repository.RoomProjectRepository
import no.timnor.app.core.repository.RoomWorkRepository
import no.timnor.app.core.repository.SpreadsheetExporter
import no.timnor.app.core.repository.StopwatchRepository
import no.timnor.app.core.repository.WorkRepository
import no.timnor.app.core.repository.BackupRepository
import no.timnor.app.core.repository.RoomBackupRepository
import no.timnor.app.core.repository.DataMutationCoordinator
import no.timnor.app.core.repository.AppMutationCoordinator
import no.timnor.app.core.service.StopwatchGateway

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
