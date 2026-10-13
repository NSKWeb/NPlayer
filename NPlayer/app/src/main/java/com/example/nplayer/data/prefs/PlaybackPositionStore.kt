package com.example.nplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.positionStore by preferencesDataStore(name = "nplayer_positions")

/**
 * Per-item resume positions, millisecond precision, keyed by media URI. Kept in
 * DataStore because positions are written every few seconds and would churn a DB.
 */
@Singleton
class PlaybackPositionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun position(itemKey: String): Long =
        context.positionStore.data.map { it[key(itemKey)] ?: 0L }.first()

    suspend fun save(itemKey: String, positionMillis: Long) {
        context.positionStore.edit { prefs ->
            if (positionMillis < 5_000) prefs.remove(key(itemKey)) else prefs[key(itemKey)] = positionMillis
        }
    }

    private fun key(itemKey: String) = longPreferencesKey("pos_$itemKey")
}
