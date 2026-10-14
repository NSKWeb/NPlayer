package com.example.nplayer.playback

import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaKind
import com.example.nplayer.data.media.normalizeFolder

/**
 * Builds the queue that plays on from the item the viewer opened. There is no server
 * playlist to read, so the queue is derived from what is already on the device: the
 * other files beside it.
 *
 * The rule is: the items in the same folder and of the same kind (video with video,
 * audio with audio), ordered the way the folder screen shows them — newest first. That
 * keeps a film folder from bleeding into the music beside it, and it makes "next" mean
 * "the next file in this folder" for both video and audio.
 */
fun buildQueue(
    items: List<MediaItem>,
    currentKey: String,
    kind: MediaKind = MediaKind.VIDEO,
): List<MediaItem> {
    val current = items.firstOrNull { it.key == currentKey } ?: return emptyList()
    val folder = normalizeFolder(current.folder)
    val siblings = items
        .filter { it.kind == kind && normalizeFolder(it.folder) == folder }
        .sortedByDescending { it.dateAddedSeconds }
    // A lone item has nothing to advance to, so the caller can fall back to a single
    // item rather than a one-entry queue.
    return if (siblings.size <= 1) emptyList() else siblings
}

/** The index of [currentKey] within [queue], or 0 when it is not there. */
fun queueIndexOf(queue: List<MediaItem>, currentKey: String): Int =
    queue.indexOfFirst { it.key == currentKey }.coerceAtLeast(0)

/** The item after the one playing, or null at the end of the queue. */
fun nextInQueue(queue: List<MediaItem>, currentKey: String): MediaItem? {
    val index = queue.indexOfFirst { it.key == currentKey }
    return if (index < 0) null else queue.getOrNull(index + 1)
}

/** The item before the one playing, or null at the start of the queue. */
fun previousInQueue(queue: List<MediaItem>, currentKey: String): MediaItem? {
    val index = queue.indexOfFirst { it.key == currentKey }
    return if (index < 0) null else queue.getOrNull(index - 1)
}
