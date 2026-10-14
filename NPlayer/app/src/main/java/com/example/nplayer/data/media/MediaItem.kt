package com.example.nplayer.data.media

import android.net.Uri

/** Whether a scanned item is a video or an audio track. */
enum class MediaKind { VIDEO, AUDIO }

/** Formats a duration as "1h 2m" or "5m"; null when the duration is unknown. */
internal fun formatDuration(millis: Long): String? {
    if (millis <= 0) return null
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** Formats a byte count with the largest sensible unit. */
internal fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
    else -> "%.0f KB".format(bytes / 1024.0)
}

/**
 * A media file found on the device. [uri] is the `content://` MediaStore location,
 * which is all playback needs — there is no server involved.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val kind: MediaKind,
    val durationMillis: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val album: String? = null,
    val artist: String? = null,
    val resolution: String? = null,
    /** Containing folder relative to storage, e.g. `"Movies/2024"`; blank at the root. */
    val folder: String = "",
) {
    /** Stable identity for navigation and resume positions. */
    val key: String get() = uri.toString()

    val durationLabel: String? get() = formatDuration(durationMillis)

    val sizeLabel: String get() = formatSize(sizeBytes)
}
