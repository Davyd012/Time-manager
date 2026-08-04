package org.examples.time_manager.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.examples.time_manager.di.DIContainer
import org.examples.time_manager.features.month_view.MonthViewModel
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.StopwatchViewModel
import java.time.LocalDate

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
