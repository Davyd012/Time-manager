package org.examples.time_manager.core.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

/** Serializes every persisted-data mutation with backup replacement. */
class DataMutationCoordinator {
    private val mutex = Mutex()
    private val timerLock = Any()
    private var timerHasUnsavedTime = false
    private var restoreInProgress = false
    private var generation = 0L

    suspend fun <T> mutate(block: suspend () -> T): T {
        val expectedGeneration = synchronized(timerLock) {
            if (restoreInProgress) throw MutationBlocked()
            generation
        }
        return mutex.withLock {
            synchronized(timerLock) {
                if (restoreInProgress || generation != expectedGeneration) throw MutationBlocked()
            }
            block()
        }
    }
    suspend fun <T> snapshot(block: suspend () -> T): T = mutex.withLock { block() }

    fun timerStarted(): Boolean = synchronized(timerLock) {
        if (restoreInProgress) false else { timerHasUnsavedTime = true; true }
    }
    fun timerCleared() = synchronized(timerLock) { timerHasUnsavedTime = false }
    fun timerObserved(hasTime: Boolean) { if (hasTime) synchronized(timerLock) { timerHasUnsavedTime = true } }

    suspend fun <T> restoreIfTimerClear(block: suspend () -> T): T? {
        synchronized(timerLock) {
            if (timerHasUnsavedTime || restoreInProgress) return null
            restoreInProgress = true
            generation++
        }
        return try { mutex.withLock { block() } } finally {
            synchronized(timerLock) { restoreInProgress = false }
        }
    }
}

object AppMutationCoordinator {
    val instance = DataMutationCoordinator()
}

private class MutationBlocked : CancellationException("A restore operation superseded this pending write")
