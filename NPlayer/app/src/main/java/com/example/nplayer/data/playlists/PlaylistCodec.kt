package com.example.nplayer.data.playlists

/**
 * Encoding for playlist storage. Playlists live in one preferences DataStore, so the
 * item list is packed into a single string. Media keys are `content://` URIs, which
 * cannot contain a newline, so that is a safe separator.
 */
private const val ITEM_SEPARATOR = "\n"

fun encodeItemKeys(keys: List<String>): String = keys.joinToString(ITEM_SEPARATOR)

fun decodeItemKeys(raw: String?): List<String> =
    raw.orEmpty().split(ITEM_SEPARATOR).filter { it.isNotEmpty() }

/** The first unused id in the `pl_N` sequence, so ids are stable and predictable. */
fun nextPlaylistId(existingIds: Set<String>): String {
    var index = 1
    while ("pl_$index" in existingIds) index++
    return "pl_$index"
}

/**
 * Makes [desired] unique against [existingNames] by appending " (2)", " (3)" and so on.
 * A blank name becomes "Playlist".
 */
fun uniquePlaylistName(desired: String, existingNames: Set<String>): String {
    val base = desired.trim().ifEmpty { "Playlist" }
    if (base !in existingNames) return base
    var suffix = 2
    while ("$base ($suffix)" in existingNames) suffix++
    return "$base ($suffix)"
}

/** Adds [itemKey] to the end of a playlist, ignoring a repeat. */
fun addKey(keys: List<String>, itemKey: String): List<String> =
    if (itemKey in keys) keys else keys + itemKey

/** Removes every occurrence of [itemKey]. */
fun removeKey(keys: List<String>, itemKey: String): List<String> = keys.filterNot { it == itemKey }
