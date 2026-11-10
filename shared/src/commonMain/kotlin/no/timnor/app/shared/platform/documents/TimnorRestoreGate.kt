package no.timnor.app.shared.platform.documents

/** Coordinates destructive restore with platform stopwatch and pending-write behavior. */
interface TimnorRestoreGate {
    /** Runs [restore] only when no unsaved stopwatch time exists; returns false otherwise. */
    suspend fun withTimerClear(restore: suspend () -> Unit): Boolean
}
