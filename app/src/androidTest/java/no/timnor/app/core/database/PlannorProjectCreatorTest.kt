package no.timnor.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import no.timnor.app.core.repository.DataMutationCoordinator
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorProjectCreator
import no.timnor.app.core.repository.PlannorSession
import no.timnor.app.core.repository.PlannorSessionStorage
import no.timnor.app.core.repository.SharedHoursLocalStore
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannorProjectCreatorTest {
    @Test
    fun createsExactlyOneLocalProjectAndLinksOnlyTheActiveAccount() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val store = SharedHoursLocalStore(database)
            val project = PlannorProjectSelection(42, 7, "Workshop", refreshedAtUtc = "now")
            store.put(42, PlannorApi.TYPE_PROJECT, "7", project, PlannorProjectSelection.serializer(), "now")
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val creator = PlannorProjectCreator(database, session, DataMutationCoordinator())

            assertTrue(creator.create(7))
            assertFalse(creator.create(7))
            val local = database.projectDao().getAll().single()
            val selection = store.get(42, PlannorApi.TYPE_PROJECT, "7", PlannorProjectSelection.serializer())!!
            assertEquals("Workshop", local.name)
            assertEquals(local.id, selection.linkedLocalProjectId ?: -1)
            assertTrue(selection.selected)
            assertEquals(null, store.get(43, PlannorApi.TYPE_PROJECT, "7", PlannorProjectSelection.serializer()))
        } finally {
            database.close()
        }
    }
}
