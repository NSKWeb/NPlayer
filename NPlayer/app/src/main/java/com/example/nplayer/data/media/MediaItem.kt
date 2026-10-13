package com.example.nplayer.data.media

import android.net.Uri

/** Whether a scanned item is a video or an audio track. */
enum class MediaKind { VIDEO, AUDIO }

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
) {
    /** Stable identity for navigation and resume positions. */
    val key: String get() = uri.toString()

    val durationLabel: String?
        get() = durationMillis.takeIf { it > 0 }?.let {
            val totalMinutes = it / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val sizeLabel: String
        get() = when {
            sizeBytes >= 1L shl 30 -> "%.1f GB".format(sizeBytes / (1L shl 30).toDouble())
            sizeBytes >= 1L shl 20 -> "%.0f MB".format(sizeBytes / (1L shl 20).toDouble())
            else -> "%.0f KB".format(sizeBytes / 1024.0)
        }
}
