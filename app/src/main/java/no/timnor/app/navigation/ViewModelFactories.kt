package no.timnor.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.timnor.app.di.DIContainer
import no.timnor.app.features.month_view.MonthViewModel
import no.timnor.app.features.root.HomeViewModel
import no.timnor.app.features.root.StopwatchViewModel
import no.timnor.app.features.settings.SettingsViewModel
import no.timnor.app.features.sharedhours.PlannorConnectionViewModel
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import java.time.LocalDate
import kotlinx.datetime.toLocalDateTime

fun getHomeViewModelFactory(diContainer: DIContainer): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                workRepository = diContainer.workRepository,
                projectRepository = diContainer.projectRepository,
                stopwatchRepository = diContainer.stopwatchRepository,
                spreadsheetExporter = diContainer.spreadsheetExporter,
            ) as T
        }
    }

fun getSharedHomeViewModelFactory(
    diContainer: DIContainer,
    initialDate: kotlinx.datetime.LocalDate = kotlin.time.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TimnorHomeViewModel(
            persistence = diContainer.sharedPersistence,
            stopwatch = diContainer.stopwatchRepository,
            loadLinkedPlannorProjects = diContainer.plannorApi::linkedLocalProjects,
            observeSharedWorkIds = diContainer.sharedWorkJournal::observeTrackedWorkIds,
            observeSharedWorkIndicators = diContainer.sharedWorkJournal::observeWorkIndicators,
            loadLinkedPlannorTasks = diContainer.plannorApi::loadTasksForLinkedLocalProject,
            initialDate = initialDate,
            calendarProjectFilterStore = diContainer.calendarProjectFilterStore,
        ) as T
    }
}

fun getMonthViewModelFactory(
    diContainer: DIContainer,
    month: LocalDate,
    selectedDay: Int,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MonthViewModel(
            month = month,
            selectedDay = selectedDay,
            workRepository = diContainer.workRepository,
            projectRepository = diContainer.projectRepository,
            spreadsheetExporter = diContainer.spreadsheetExporter,
        ) as T
    }
}

fun getStopwatchViewModelFactory(diContainer: DIContainer): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return StopwatchViewModel(
                projectRepository = diContainer.projectRepository,
                workRepository = diContainer.workRepository,
                stopwatchRepository = diContainer.stopwatchRepository,
            ) as T
        }
    }

fun getSettingsViewModelFactory(diContainer: DIContainer): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(diContainer.backupRepository, diContainer.stopwatchRepository) as T
        }
    }

fun getSharedSettingsViewModelFactory(diContainer: DIContainer): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TimnorSettingsViewModel(
                persistence = diContainer.sharedPersistence,
                restoreGate = diContainer.mutationCoordinator,
            ) as T
        }
    }

fun getPlannorConnectionViewModelFactory(diContainer: DIContainer): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PlannorConnectionViewModel(
                diContainer.plannorApi,
                diContainer.sharedPersistence,
                diContainer.plannorProjectCreator::create,
                diContainer.sharedWorkSync::sync,
                loadSharedWorkReviews = {
                    val entries = diContainer.plannorApi.loadPersonalEntries()
                    val allocations = diContainer.plannorApi.refreshProjectSnapshots()
                    diContainer.sharedWorkJournal.reviewItems(
                        diContainer.sharedPersistence.snapshotAll().works,
                        entries,
                        allocations,
                    )
                },
                resolveSharedWork = { localWorkId, resolution ->
                    val entries = diContainer.plannorApi.loadPersonalEntries()
                    val allocations = diContainer.plannorApi.refreshProjectSnapshots()
                    diContainer.sharedWorkJournal.resolve(localWorkId, resolution, entries, allocations)
                },
            ) as T
        }
    }
