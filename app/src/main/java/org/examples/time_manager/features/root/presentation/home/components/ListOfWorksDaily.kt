package org.examples.time_manager.features.root.presentation.home.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work

@SuppressLint("DefaultLocale")
@Composable
fun ListOfWorks(
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
                    text = "Ingen arbeidsregnskap",
                    style = style.titleMedium.copy(color = colors.onSurface)
                )
            }
        }
        return
    }

    works.forEach {
        var text = "0"
        if (it.time > 0) text += it.time / 60
        if (it.time % 60 > 0) text += "." + it.time % 60

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Gray)
                .height(60.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(Color.Yellow)
                    .clip(RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                projects.elementAt(it.project).name,
                style = style.titleMedium.copy(color = colors.onPrimaryContainer)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text,
                style = style.bodyLarge.copy(color = colors.onPrimaryContainer)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}