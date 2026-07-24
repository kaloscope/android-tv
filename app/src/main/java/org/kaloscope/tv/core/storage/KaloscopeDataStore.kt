package org.kaloscope.tv.core.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.kaloscopeDataStore by preferencesDataStore(name = "kaloscope")
