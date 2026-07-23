package org.kaloscope.tv.core.storage

/**
 * Persists encrypted tokens under server-specific keys.
 */
interface SessionStore {
    suspend fun getToken(serverId: String): String?

    suspend fun setToken(serverId: String, token: String)

    suspend fun clearToken(serverId: String)
}
