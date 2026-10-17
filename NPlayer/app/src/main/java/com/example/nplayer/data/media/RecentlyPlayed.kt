package com.example.nplayer.data.media

/**
 * Records that [itemKey] was played now, returning the new newest-first list.
 *
 * Replaying an item moves it to the front rather than adding a second entry, and the
 * list is capped at [limit] so the history cannot grow without bound.
 */
fun recentlyPlayedOrder(
    existing: List<String>,
    playedKey: String,
    limit: Int,
): List<String> {
    if (limit <= 0) return emptyList()
    return (listOf(playedKey) + existing.filterNot { it == playedKey }).take(limit)
}
