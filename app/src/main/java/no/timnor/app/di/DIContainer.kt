package no.timnor.app.di

import android.content.Context
import no.timnor.app.core.database.Database
import no.timnor.app.core.database.getDatabase
import no.timnor.app.core.database.RoomPersistenceStore
import no.timnor.app.core.database.CoordinatedTimnorPersistence
import no.timnor.app.core.repository.PoiSpreadsheetExporter
import no.timnor.app.core.repository.ProjectRepository
import no.timnor.app.core.repository.RoomProjectRepository
import no.timnor.app.core.repository.RoomWorkRepository
import no.timnor.app.core.repository.SpreadsheetExporter
import no.timnor.app.core.repository.WorkRepository
import no.timnor.app.core.repository.BackupRepository
import no.timnor.app.core.repository.RoomBackupRepository
import no.timnor.app.core.repository.DataMutationCoordinator
import no.timnor.app.core.repository.AppMutationCoordinator
import no.timnor.app.core.repository.SharedHoursLocalStore
import no.timnor.app.core.repository.PlannorSessionStore
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorProjectCreator
import no.timnor.app.core.repository.SharedWorkJournal
import no.timnor.app.core.repository.SharedWorkSync
import no.timnor.app.core.service.StopwatchGateway
import no.timnor.app.shared.data.persistence.CalendarProjectFilterStore
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform

class DIContainer(context: Context) {
    private val applicationContext = context.applicationContext
    val calendarProjectFilterStore: CalendarProjectFilterStore by lazy {
        val preferences = applicationContext.getSharedPreferences("calendar", Context.MODE_PRIVATE)
        CalendarProjectFilterStore(
            read = { preferences.getString("selected_project_ids", null) },
            write = { preferences.edit().putString("selected_project_ids", it).apply() },
        )
    }

    val database: Database by lazy { getDatabase(applicationContext) }
    val sharedWorkJournal: SharedWorkJournal by lazy { SharedWorkJournal(database, plannorSessionStore) }
    val persistence: RoomPersistenceStore by lazy { RoomPersistenceStore(database, sharedWorkJournal) }
    val sharedHoursLocalStore: SharedHoursLocalStore by lazy { SharedHoursLocalStore(database) }
    val plannorSessionStore: PlannorSessionStore by lazy { PlannorSessionStore(applicationContext) }
    val plannorApi: PlannorApi by lazy { PlannorApi(plannorSessionStore, sharedHoursLocalStore) }
    val sharedWorkSync: SharedWorkSync by lazy { SharedWorkSync(sharedWorkJournal, plannorApi) }
    val plannorProjectCreator: PlannorProjectCreator by lazy {
        PlannorProjectCreator(database, plannorSessionStore, mutationCoordinator)
    }
    val sharedPersistence: CoordinatedTimnorPersistence by lazy {
        CoordinatedTimnorPersistence(persistence, mutationCoordinator)
    }
    val mutationCoordinator: DataMutationCoordinator = AppMutationCoordinator.instance
    val workRepository: WorkRepository by lazy { RoomWorkRepository(persistence, mutationCoordinator) }
    val projectRepository: ProjectRepository by lazy { RoomProjectRepository(persistence, mutationCoordinator) }
    val backupRepository: BackupRepository by lazy {
        RoomBackupRepository(database, persistence, applicationContext.contentResolver, mutationCoordinator)
    }
    val spreadsheetExporter: SpreadsheetExporter by lazy { PoiSpreadsheetExporter(applicationContext) }
    val stopwatchRepository: StopwatchPlatform by lazy {
        StopwatchGateway(applicationContext, mutationCoordinator).also { it.bind() }
    }
}
