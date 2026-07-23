package org.kaloscope.tv.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "kaloscope_sessions")

@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: TokenCipher,
) : SessionStore {
    override suspend fun getToken(serverId: String): String? {
        val encrypted = context.sessionDataStore.data.first()[key(serverId)] ?: return null
        return cipher.decrypt(encrypted)
    }

    override suspend fun setToken(serverId: String, token: String) {
        context.sessionDataStore.edit { preferences ->
            // DataStore receives ciphertext only; plaintext remains process-local.
            preferences[key(serverId)] = cipher.encrypt(token)
        }
    }

    override suspend fun clearToken(serverId: String) {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(key(serverId))
        }
    }

    // Separate keys prevent a session from being reused after switching servers.
    private fun key(serverId: String) = stringPreferencesKey("token_$serverId")
}
