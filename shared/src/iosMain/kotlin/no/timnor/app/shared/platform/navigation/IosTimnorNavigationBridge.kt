package no.timnor.app.shared.platform.navigation

/** Edge-swipe Back entry point used by the native iOS host for shared nested routes. */
object IosTimnorNavigationBridge {
    private var canGoBack = false
    private var backAction: (() -> Unit)? = null

    fun update(canGoBack: Boolean, backAction: () -> Unit) {
        this.canGoBack = canGoBack
        this.backAction = backAction
    }

    fun canHandleBack(): Boolean = canGoBack

    fun handleBack() {
        if (canGoBack) backAction?.invoke()
    }
}
