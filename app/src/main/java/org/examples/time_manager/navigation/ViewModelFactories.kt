package org.examples.time_manager.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.examples.time_manager.di.DIContainer
import org.examples.time_manager.features.calendar.CalendarViewModel
import org.examples.time_manager.features.month_view.MonthViewModel
import org.examples.time_manager.features.root.HomeViewModel
import java.time.LocalDate


fun getHomeViewModelFactory(
    diContainer: DIContainer,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                stopwatchService = diContainer.stopwatchService,
            ) as T
        }
    }
}

fun getCalendarViewModelFactory(
    diContainer: DIContainer,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(
            ) as T
        }
    }
}

fun getMonthViewModelFactory(
    diContainer: DIContainer,
    month: LocalDate,
    selectedDay: Int,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MonthViewModel(
                month = month,
                selectedDay = selectedDay
            ) as T
        }
    }
}