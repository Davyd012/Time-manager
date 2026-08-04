package org.examples.time_manager

import android.app.Application
import org.examples.time_manager.di.DIContainer

class App : Application() {
    lateinit var diContainer: DIContainer
        private set


    var statusBarHeight: Dp = 0.dp
    var hasCutOut: Boolean = false
    
    val context: Context
        get() = instance.applicationContext

    override fun onCreate() {
        super.onCreate()
        diContainer = DIContainer(this)
    }
}
