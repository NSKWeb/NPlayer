package com.example.nplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nplayer.data.media.recentlyPlayedOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recentStore by preferencesDataStore(name = "nplayer_recent")

/** How many items the Recently played list keeps. */
const val RECENTLY_PLAYED_LIMIT = 20

/**
 * The item keys played most recently, newest first, kept on the device. Written when an
 * item starts playing, so the Home screen can offer a way back to what was on.
 */
@Singleton
class RecentlyPlayedStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keysKey = stringPreferencesKey("recent_keys")

    /** Newest-first keys, as stored. Unknown keys are filtered out by the caller. */
    val keys: Flow<List<String>> = context.recentStore.data.map { prefs ->
        decode(prefs[keysKey])
    }

    suspend fun current(): List<String> = keys.first()

    /** Records [itemKey] as played now, moving it to the front of the list. */
    suspend fun record(itemKey: String) {
        context.recentStore.edit { prefs ->
            val updated = recentlyPlayedOrder(
                existing = decode(prefs[keysKey]),
                playedKey = itemKey,
                limit = RECENTLY_PLAYED_LIMIT,
            )
            prefs[keysKey] = encode(updated)
        }
    }

    suspend fun clear() {
        context.recentStore.edit { it.remove(keysKey) }
    }
}

/**
 * Keys are stored newline-separated. A `content://` URI cannot contain a newline, so it
 * is a safe separator that needs no escaping, unlike a comma.
 */
private fun encode(keys: List<String>): String = keys.joinToString("\n")

private fun decode(raw: String?): List<String> =
    raw?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
