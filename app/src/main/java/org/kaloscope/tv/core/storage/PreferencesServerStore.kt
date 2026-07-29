package org.kaloscope.tv.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.model.SavedServer

@Singleton
class PreferencesServerStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : ServerStore {
    override suspend fun getServers(): List<SavedServer> =
        decodeServers(dataStore.data.first()[SERVERS]).map(StoredServer::toModel)

    override suspend fun save(server: SavedServer) {
        dataStore.edit { preferences ->
            val current = decodeServers(preferences[SERVERS])
            val updated = current
                .filterNot { it.id == server.id }
                .plus(StoredServer.fromModel(server))
            preferences[SERVERS] = json.encodeToString(updated)
        }
    }

    override suspend fun delete(serverId: String): List<SavedServer> {
        var remaining = emptyList<StoredServer>()
        dataStore.edit { preferences ->
            val current = decodeServers(preferences[SERVERS])
            remaining = current.filterNot { it.id == serverId }
            preferences[SERVERS] = json.encodeToString(remaining)
            if (preferences[ACTIVE_SERVER] == serverId) {
                preferences.remove(ACTIVE_SERVER)
            }
        }
        return remaining.map(StoredServer::toModel)
    }

    override suspend fun getActiveServerId(): String? =
        dataStore.data.first()[ACTIVE_SERVER]

    override suspend fun setActiveServerId(serverId: String) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_SERVER] = serverId
        }
    }

    // Treat corrupt metadata as empty so reads stay safe and the next write repairs the list.
    private fun decodeServers(encoded: String?): List<StoredServer> =
        encoded?.let {
            runCatching {
                json.decodeFromString<List<StoredServer>>(it)
            }.getOrNull()
        }.orEmpty()

    private companion object {
        val SERVERS = stringPreferencesKey("servers")
        val ACTIVE_SERVER = stringPreferencesKey("active_server")
    }
}

@Serializable
private data class StoredServer(
    val id: String,
    val name: String,
    val origin: String,
) {
    fun toModel() = SavedServer(id = id, name = name, origin = origin)

    companion object {
        fun fromModel(server: SavedServer) = StoredServer(
            id = server.id,
            name = server.name,
            origin = server.origin,
        )
    }
}
