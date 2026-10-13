package com.example.nplayer.data.local

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
 * Per-item resume positions, millisecond precision. Kept in DataStore (small,
 * frequent writes) rather than Room so position updates never churn the DB.
 */
@Singleton
class PlaybackPositionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun position(itemId: String): Long =
        context.positionStore.data.map { it[key(itemId)] ?: 0L }.first()

    suspend fun save(itemId: String, positionMillis: Long) {
        context.positionStore.edit { prefs ->
            if (positionMillis < 5_000) prefs.remove(key(itemId)) else prefs[key(itemId)] = positionMillis
        }
    }

    private fun key(itemId: String) = longPreferencesKey("pos_$itemId")
}
