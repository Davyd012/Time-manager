package no.timnor.app.core.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataMutationCoordinatorTest {
    @Test
    fun activeTimerBlocksRestore() = runBlocking {
        val coordinator = DataMutationCoordinator()
        assertTrue(coordinator.timerStarted())
        var replaced = false

        val result = coordinator.restoreIfTimerClear { replaced = true }

        assertNull(result)
        assertFalse(replaced)
    }

    @Test
    fun restoreGateRejectsTimerStart() = runBlocking {
        val coordinator = DataMutationCoordinator()
        var startAllowed = true

        val completed = coordinator.restoreIfTimerClear {
            startAllowed = coordinator.timerStarted()
            true
        }

        assertTrue(completed == true)
        assertFalse(startAllowed)
        assertTrue(coordinator.timerStarted())
    }
}
