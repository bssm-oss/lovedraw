package com.example.couplecanvas.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.overlayDataStore by preferencesDataStore("overlay_state")

class OverlayStateStore(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
    }

    val enabled: Flow<Boolean> = context.overlayDataStore.data.map { prefs ->
        prefs[Keys.enabled] ?: true
    }

    suspend fun isEnabled(): Boolean = enabled.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.overlayDataStore.edit { prefs ->
            prefs[Keys.enabled] = enabled
        }
    }
}
