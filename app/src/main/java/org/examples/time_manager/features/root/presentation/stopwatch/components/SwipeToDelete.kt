package org.examples.time_manager.features.root.presentation.stopwatch.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.examples.time_manager.ui.theme.deleteIcon
import org.examples.time_manager.ui.theme.editIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    animationDuration: Int = 500,
    color: Color,
    removeAction: () -> Unit,
    modifyProject: () -> Unit,
    content: @Composable () -> Unit
) {
    var isRemoved by remember {
        mutableStateOf(false)
    }
    var modify by remember {
        mutableStateOf(false)
    }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                true
            } else {
                modify = true
                false
            }
        }
    )

    LaunchedEffect(key1 = isRemoved, key2 = modify) {
        if (isRemoved) {
            delay(animationDuration.toLong())
            removeAction()
        }
        if (modify) {
            modifyProject()
            modify = false
        }
    }

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = animationDuration),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = {
                DeleteBackground(color = color)
            },
            content = { content() },
//            enableDismissFromStartToEnd = false,
        )
    }
}

@Composable
fun DeleteBackground(
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = editIcon(),
            contentDescription = null,
            tint = color
        )
        Icon(
            imageVector = deleteIcon(),
            contentDescription = null,
            tint = color
        )
    }
}