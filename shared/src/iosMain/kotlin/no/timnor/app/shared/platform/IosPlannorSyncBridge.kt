package no.timnor.app.shared.platform

import no.timnor.app.shared.features.shared_hours.domain.PlannorAccountViewModel

/** SwiftUI foreground and connectivity events reuse the shared coalesced sync entry point. */
object IosPlannorSyncBridge {
    private var viewModel: PlannorAccountViewModel? = null

    fun attach(value: PlannorAccountViewModel) {
        viewModel = value
    }

    fun detach(value: PlannorAccountViewModel) {
        if (viewModel === value) viewModel = null
    }

    fun retrySync() {
        viewModel?.retrySync()
    }
}
