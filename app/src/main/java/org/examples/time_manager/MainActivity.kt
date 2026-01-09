package org.examples.time_manager

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import org.examples.time_manager.core.service.StopwatchService
import org.examples.time_manager.navigation.AppNavHost
import org.examples.time_manager.ui.theme.TimeMangerTheme

class MainActivity : ComponentActivity() {
    private var isBound by mutableStateOf(false)
    private var stopwatchService: StopwatchService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("MainViewModel", "Bound stopwatchService activity")
            val binder = service as StopwatchService.StopwatchBinder
            stopwatchService = binder.getService()
            App.diContainer.stopwatchService = stopwatchService
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("MainViewModel", "Bound error activity")
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, StopwatchService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainViewModel", "Stopwatch service state: ${(stopwatchService == null)}")
        Log.d("MainViewModel", "Is bound: $isBound")

        makeFullscreen()

        setContent {
            TimeMangerTheme {
                AppNavHost(
                    diContainer = App.diContainer,
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestPermissions(vararg permissions: String) {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            result.entries.forEach {
                Log.d("MainActivity", "${it.key} = ${it.value}")
            }
        }
        requestPermissionLauncher.launch(permissions.asList().toTypedArray())
    }


    private fun makeFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            WindowCompat.setDecorFitsSystemWindows(window, false)
            return
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }
}
