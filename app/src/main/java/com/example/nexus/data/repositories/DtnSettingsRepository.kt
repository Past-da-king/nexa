package com.example.nexus.data.repositories

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nexus.models.DtnSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dtn_settings")

@Singleton
class DtnSettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val TTL = longPreferencesKey("ttl")
        val HOP_COUNT = intPreferencesKey("hop_count")
        val STORAGE_LIMIT = intPreferencesKey("storage_limit")
    }

    val dtnSettings: Flow<DtnSettings> = context.dataStore.data
        .map { preferences ->
            val ttl = preferences[PreferencesKeys.TTL]
            val hopCount = preferences[PreferencesKeys.HOP_COUNT]
            val storageLimit = preferences[PreferencesKeys.STORAGE_LIMIT]

            if (ttl == null || hopCount == null || storageLimit == null) {
                Log.d("DtnSettingsRepo", "Applying default DTN settings. Hop Count: 15")
            }

            DtnSettings(
                ttl = ttl ?: 86400_000L, // 24 hours
                hopCount = hopCount ?: 15, // Default hop count set to 15
                storageLimit = storageLimit ?: 100
            )
        }

    suspend fun updateDtnSettings(dtnSettings: DtnSettings) {
        context.dataStore.edit { preferences ->
            Log.d("DtnSettingsRepo", "Updating DTN settings to: ${dtnSettings}")
            preferences[PreferencesKeys.TTL] = dtnSettings.ttl
            preferences[PreferencesKeys.HOP_COUNT] = dtnSettings.hopCount
            preferences[PreferencesKeys.STORAGE_LIMIT] = dtnSettings.storageLimit
        }
    }
}
