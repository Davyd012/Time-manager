package org.examples.time_manager.features.root.presentation.home.components

import android.annotation.SuppressLint
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

    if (works.isEmpty()) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/empty.json")
        )

        val progress by animateLottieCompositionAsState(
            composition,
            iterations = LottieConstants.IterateForever,
            restartOnPlay = false
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LottieAnimation(
                    composition,
                    progress = { progress },
                    modifier = Modifier.size(400.dp)
                )
                Text(
                    text = stringResource(R.string.no_work_logs),
                    style = style.titleMedium.copy(color = colors.onSurface)
                )
            }
        }
        return
    }


    val sortedMap = works
        .groupBy { it.date.hour }
        .toSortedMap()

    Column(
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(topEnd = 30.dp, topStart = 30.dp))
            .fillMaxHeight()
            .background(colors.tertiaryContainer)
            .padding(10.dp)
    ) {
        sortedMap.onEachIndexed { index, time ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    String.format("%02d", time.key),
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
                    time.value.forEach { work ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .clickable {
                                    showWork(works.indexOf(work))
                                }
                                .background(colors.onSecondaryContainer)
                                .height(60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight()
                                    .background(colors.secondaryContainer)
                                    .clip(RoundedCornerShape(5.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    projects.firstOrNull { p -> p.id == work.project }?.name
                                        ?: stringResource(R.string.no_project),
                                    style = style.titleMedium.copy(color = colors.secondaryContainer)
                                )
                                if (work.description.isNotEmpty()) Text(
                                    work.description.toString(),
                                    style = style.bodyMedium.copy(color = colors.secondaryContainer)
                                )
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
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
//                        .height(1.5.dp)
//                        .background(colors.onTertiaryContainer),
                )
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
