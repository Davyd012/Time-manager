package no.timnor.app.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import no.timnor.app.features.sharedhours.PlannorConnectionViewModel

@Composable
internal fun SharedWorkSyncTriggers(viewModel: PlannorConnectionViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.requestSync()
        }
        lifecycle.addObserver(observer)
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = viewModel.requestSync()
        }
        val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
        onDispose {
            lifecycle.removeObserver(observer)
            if (registered) manager.unregisterNetworkCallback(callback)
        }
    }
}
