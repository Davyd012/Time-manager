package org.examples.time_manager.features.root.presentation.home.preview

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.features.root.HomeState
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.domain.DatesController
import org.examples.time_manager.ui.theme.LightColorScheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@SuppressLint("NewApi")
@Preview
@Composable
fun MainPagePreview() {
    val colors = LightColorScheme
    val vm = HomeViewModel()
    val state by vm.state.collectAsState()

    val listState = rememberLazyListState()
    val localDate = LocalDateTime.now().minusHours(3)
    val works = listOf(
        Work(
            description = "",
            date = LocalDateTime.now(),
            project = 0,
            task = 0,
            time = 36456,
            id = 0
        ),Work(
            description = "",
            date = LocalDateTime.now(),
            project = 0,
            task = 0,
            time = 36456,
            id = 0
        ),
        Work(
            description = "",
            date = localDate,
            project = 0,
            task = 0,
            time = 156,
            id = 0
        ),
    )
    val projects =
        listOf(Project(description = "Some kind of a first project", name = "Alley", id = 0))
//    val works by state.workQueries.collectAsState(initial = emptyList())
//    val projects by state.projects.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.background(colors.inverseSurface).fillMaxSize()) {
            HeaderWidget(it, colors, state, listState, vm)
            Spacer(modifier = Modifier.height(5.dp))

            ListOfWorks(
                works = works,
                projects = projects
            )
        }
    }
}