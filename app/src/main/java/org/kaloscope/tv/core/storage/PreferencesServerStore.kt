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
    override suspend fun getServers(): List<SavedServer> {
        val encoded = dataStore.data.first()[SERVERS] ?: return emptyList()
        // Corrupt local metadata must not crash startup or expose stale servers.
        return runCatching {
            json.decodeFromString<List<StoredServer>>(encoded).map(StoredServer::toModel)
        }.getOrElse { emptyList() }
    }

    override suspend fun save(server: SavedServer) {
        dataStore.edit { preferences ->
            // A corrupt list is replaced by the newly verified server.
            val current = preferences[SERVERS]
                ?.let { encoded ->
                    runCatching {
                        json.decodeFromString<List<StoredServer>>(encoded)
                    }.getOrNull()
                }
                .orEmpty()
            val updated = current
                .filterNot { it.id == server.id }
                .plus(StoredServer.fromModel(server))
            preferences[SERVERS] = json.encodeToString(updated)
        }
    }

    override suspend fun getActiveServerId(): String? =
        dataStore.data.first()[ACTIVE_SERVER]

    override suspend fun setActiveServerId(serverId: String) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_SERVER] = serverId
        }
    }

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
