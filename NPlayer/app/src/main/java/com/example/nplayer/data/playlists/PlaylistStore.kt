package com.example.nplayer.data.playlists

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playlistStore by preferencesDataStore(name = "nplayer_playlists")

/** Playlists, kept on the device in DataStore. There is no server copy. */
@Singleton
class PlaylistStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nameKey = stringPreferencesKey("playlist_name")
    private val itemsKey = stringPreferencesKey("playlist_items")

    val playlists: Flow<List<Playlist>> = context.playlistStore.data.map { prefs ->
        prefs.asMap()
            .filterKeys { it.name.startsWith("$nameKey.") }
            .mapNotNull { (key, value) ->
                val id = key.name.removePrefix("$nameKey.")
                val name = value as? String ?: return@mapNotNull null
                Playlist(
                    id = id,
                    name = name,
                    itemKeys = decodeItemKeys(prefs[itemsKeyFor(id)]),
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun current(): List<Playlist> = playlists.first()

    /** Creates a playlist whose name is unique, returning the new playlist. */
    suspend fun create(desiredName: String): Playlist {
        val existing = current()
        val name = uniquePlaylistName(desiredName, existing.map { it.name }.toSet())
        val id = nextPlaylistId(existing.map { it.id }.toSet())
        context.playlistStore.edit { it[nameKeyFor(id)] = name }
        return Playlist(id = id, name = name)
    }

    suspend fun rename(id: String, desiredName: String) {
        val existing = current()
        if (existing.none { it.id == id }) return
        val others = existing.filterNot { it.id == id }.map { it.name }.toSet()
        context.playlistStore.edit { it[nameKeyFor(id)] = uniquePlaylistName(desiredName, others) }
    }

    suspend fun delete(id: String) {
        context.playlistStore.edit { prefs ->
            prefs.remove(nameKeyFor(id))
            prefs.remove(itemsKeyFor(id))
        }
    }

    suspend fun addItem(id: String, itemKey: String) {
        context.playlistStore.edit { prefs ->
            val updated = addKey(decodeItemKeys(prefs[itemsKeyFor(id)]), itemKey)
            prefs[itemsKeyFor(id)] = encodeItemKeys(updated)
        }
    }

    suspend fun removeItem(id: String, itemKey: String) {
        context.playlistStore.edit { prefs ->
            val updated = removeKey(decodeItemKeys(prefs[itemsKeyFor(id)]), itemKey)
            prefs[itemsKeyFor(id)] = encodeItemKeys(updated)
        }
    }

    private fun nameKeyFor(id: String) = stringPreferencesKey("$nameKey.$id")

    private fun itemsKeyFor(id: String) = stringPreferencesKey("$itemsKey.$id")
}
