package org.examples.time_manager

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import org.examples.time_manager.navigation.AppNavHost
import org.examples.time_manager.ui.theme.TimeMangerTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestStoragePermissions()

        setContent {
            TimeMangerTheme {
                AppNavHost(
                    diContainer = App.diContainer,
                    navController = rememberNavController()
                )
            }
        }
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