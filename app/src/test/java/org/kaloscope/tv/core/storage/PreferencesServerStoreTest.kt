package org.kaloscope.tv.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.kaloscope.tv.core.model.SavedServer

class PreferencesServerStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `delete removes only the target and preserves order`() = runTest {
        val store = createStore()
        val first = server("first")
        val target = server("target")
        val last = server("last")
        listOf(first, target, last).forEach { store.save(it) }

        val remaining = store.delete(target.id)

        assertEquals(listOf(first, last), remaining)
        assertEquals(listOf(first, last), store.getServers())
    }

    @Test
    fun `deleting a non-active server preserves the active id`() = runTest {
        val store = createStore()
        val active = server("active")
        val target = server("target")
        store.save(active)
        store.save(target)
        store.setActiveServerId(active.id)

        store.delete(target.id)

        assertEquals(active.id, store.getActiveServerId())
    }

    @Test
    fun `deleting the active last server clears active id`() = runTest {
        val store = createStore()
        val server = server("only")
        store.save(server)
        store.setActiveServerId(server.id)

        val remaining = store.delete(server.id)

        assertEquals(emptyList<SavedServer>(), remaining)
        assertNull(store.getActiveServerId())
    }

    private fun TestScope.createStore(): PreferencesServerStore {
        val dataFile = File(temporaryFolder.newFolder(), "servers.preferences_pb")
        return PreferencesServerStore(
            dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { dataFile },
            ),
            json = Json,
        )
    }

    private fun server(id: String) = SavedServer(
        id = id,
        name = "Server $id",
        origin = "https://$id.example",
    )
}
