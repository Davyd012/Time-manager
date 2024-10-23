package org.examples.time_manager

import android.app.Application
import android.content.Context
import org.examples.time_manager.di.DIContainer

class App: Application() {
    companion object {
        lateinit var instance: App
            private set

        val diContainer = DIContainer()

        fun getAppContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}