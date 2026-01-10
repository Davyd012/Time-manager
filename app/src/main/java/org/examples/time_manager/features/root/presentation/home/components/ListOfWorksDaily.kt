package org.examples.time_manager.features.root.presentation.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.ui.res.stringResource
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work

@SuppressLint("DefaultLocale")
@Composable
fun ListOfWorks(
    showWork: (Int) -> Unit,
    works: List<Work>,
    projects: List<Project>,
) {
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    AnimatedContent(
        targetState = works.isEmpty(),
        label = "empty_to_list",
        transitionSpec = {
            val inFrom =
                if (targetState) { h: Int -> h / 12 } else { h: Int -> -h / 12 }

            (fadeIn(tween(180)) +
                    slideInVertically(
                        animationSpec = tween(220),
                        initialOffsetY = inFrom
                    ))
                .togetherWith(ExitTransition.None)
                .using(SizeTransform(clip = false))
        }
    ) { empty ->
        if (empty) {
            EmptyWorksState()
        } else {
            WorksListState(
                works = works,
                projects = projects,
                showWork = showWork,
                formatTime = { it: Long -> "" },
                formatHoursFromSeconds = { it: Long -> "" }
            )
        }
    }
}

@Composable
private fun EmptyWorksState(
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography

    // Lottie (som du har)
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/empty.json")
    )
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        restartOnPlay = false
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(400.dp)
            )
            Text(
                text = stringResource(R.string.no_work_logs),
                style = style.titleMedium.copy(color = colors.onSurface)
            )
        }
    }
}

@Composable
private fun WorksListState(
    works: List<Work>,
    projects: List<Project>,
    showWork: (Int) -> Unit,
    formatTime: (Long) -> String,
    formatHoursFromSeconds: (Long) -> String
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography

    val sortedMap = remember(works) {
        works.groupBy { it.date.hour }.toSortedMap()
    }

    Column(
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(topEnd = 30.dp, topStart = 30.dp))
            .fillMaxHeight()
            .background(colors.tertiaryContainer)
            .padding(10.dp)
    ) {
        sortedMap.entries.forEachIndexed { index, entry ->
            val hour = entry.key
            val hourWorks = entry.value

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    String.format("%02d", hour),
                    style = style.titleLarge.copy(
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .width(25.dp)
                        .height(2.dp)
                        .background(colors.onPrimaryContainer)
                )
                Spacer(modifier = Modifier.width(5.dp))

                Column(modifier = Modifier.weight(1f)) {
                    hourWorks.forEach { work ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .clickable { showWork(works.indexOf(work)) }
                                .background(colors.onSecondaryContainer)
                                .height(60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight()
                                    .background(colors.secondaryContainer)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    projects.firstOrNull { p -> p.id == work.project }?.name
                                        ?: stringResource(R.string.no_project),
                                    style = style.titleMedium.copy(color = colors.secondaryContainer)
                                )
                                if (work.description.isNotEmpty()) {
                                    Text(
                                        work.description,
                                        style = style.bodyMedium.copy(color = colors.secondaryContainer)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                formatTime(work.time),
                                style = style.bodyLarge.copy(color = colors.secondaryContainer)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))
            }

            if (index < sortedMap.size - 1) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}


@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String {
    return when {
        seconds >= 3600 -> String.format("%.1fh", seconds / 3600.0) // hours
        seconds >= 60 -> String.format("%.1fm", seconds / 60.0)     // minutes
        else -> "$seconds s"                                         // seconds
    }
}
