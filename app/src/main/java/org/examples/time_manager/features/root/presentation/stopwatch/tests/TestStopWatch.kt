package org.examples.time_manager.features.root.presentation.stopwatch.tests

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.presentation.stopwatch.Stopwatch

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun TestStopWatch() {
    Stopwatch(vm = HomeViewModel(), modifier = Modifier)
}