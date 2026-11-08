package no.timnor.app

import android.app.Application
import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import no.timnor.app.di.DIContainer

class App : Application() {
    lateinit var diContainer: DIContainer
        private set

    companion object {
        lateinit var instance: App
            private set

        var statusBarHeight: Dp = 0.dp
        var hasCutOut: Boolean = false

        val context: Context
            get() = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        diContainer = DIContainer(this)
    }
}
