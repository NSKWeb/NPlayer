package com.example.nplayer.data.playlists

/**
 * A user-made playlist. Items are stored as media keys (`content://` URIs), so a
 * playlist never duplicates file data and an item that is deleted simply drops out.
 */
data class Playlist(
    val id: String,
    val name: String,
    val itemKeys: List<String> = emptyList(),
) {
    val size: Int get() = itemKeys.size
}
