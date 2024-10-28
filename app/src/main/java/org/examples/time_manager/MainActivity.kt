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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import org.examples.time_manager.core.service.StopwatchService
import org.examples.time_manager.navigation.AppNavHost
import org.examples.time_manager.ui.theme.TimeMangerTheme

class MainActivity : ComponentActivity() {
    private var isBound by mutableStateOf(false)
    private var stopwatchService: StopwatchService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("MainActivity", "Bound stopwatchService activity")
            val binder = service as StopwatchService.StopwatchBinder
            stopwatchService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("MainActivity", "Bound error activity")
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "Starting activity")
        Intent(this, StopwatchService::class.java).also { intent ->
            Log.d("MainActivity", "Binding stopwatchService activity")

            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeMangerTheme {
                AppNavHost(
                    diContainer = App.diContainer,
                    navController = rememberNavController(),
                    stopwatchService = stopwatchService,
                )

            }
        }
        requestPermissions(Manifest.permission.POST_NOTIFICATIONS)
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

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestStoragePermissions() {
        val permissions = arrayOf(
            Manifest.permission.MANAGE_MEDIA,
            Manifest.permission.MANAGE_DOCUMENTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, 1)
    }
}