package com.example.nplayer.playback

import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

/**
 * An audio track that can be played for the current item: the tracks muxed into the
 * container. Unlike subtitles there is no sidecar form, because an audio track only
 * exists inside the file.
 */
data class AudioTrack(
    val id: String,
    val label: String,
    val language: String? = null,
    val mimeType: String? = null,
    val channels: Int = 0,
    /** True when this is the track playing now. */
    val selected: Boolean = false,
)

/**
 * The audio tracks on [tracks], in the order the container lists them. A file with one
 * audio track still yields one entry, so the picker can show what is playing.
 */
fun buildAudioTracks(tracks: Tracks): List<AudioTrack> =
    tracks.groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
        .mapIndexed { index, group ->
            val format = group.getTrackFormat(0)
            AudioTrack(
                id = group.mediaTrackGroup.id,
                label = audioTrackLabel(format.label, format.language, format.channelCount, index),
                language = format.language,
                mimeType = format.sampleMimeType,
                channels = format.channelCount,
                selected = group.isTrackSelected(0),
            )
        }

/**
 * A readable name for an audio track. The container's own label wins, then the language,
 * then a channel description; the index is the fallback so a track is never nameless.
 */
fun audioTrackLabel(
    label: String?,
    language: String?,
    channelCount: Int,
    index: Int,
): String {
    val channelText = when (channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> null
    }
    val base = label?.takeIf { it.isNotBlank() }
        ?: language?.takeIf { it.isNotBlank() }?.let { formatLanguage(it) }
        ?: channelText
        ?: "Track ${index + 1}"
    // Add the channel layout when it says more than the label already does.
    return if (channelText != null && !base.contains(channelText, ignoreCase = true)) {
        "$base · $channelText"
    } else {
        base
    }
}

/** Turns an ISO code such as "eng" into "English"; an unknown code is left alone. */
private fun formatLanguage(tag: String): String =
    com.example.nplayer.data.media.languageDisplayName(tag) ?: tag

/**
 * The override that selects [track] out of [groups], or null when the group is gone
 * (for example after the queue advanced to a file with different tracks).
 */
fun audioOverrideFor(groups: List<TrackGroup>, track: AudioTrack): TrackSelectionOverride? =
    groups.firstOrNull { it.id == track.id }?.let { TrackSelectionOverride(it, 0) }
