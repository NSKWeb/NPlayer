package com.example.nplayer.playback

import com.example.nplayer.data.media.sidecarLabel
import com.example.nplayer.data.media.sidecarLanguage
import com.example.nplayer.data.media.sidecarMatchesMedia
import com.example.nplayer.data.media.sidecarMimeType

/**
 * A subtitle track that can be shown for the current item: a sidecar file sitting
 * next to the media, or a text track already embedded in the container.
 */
data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String? = null,
    val mimeType: String? = null,
    /** True for a text track muxed into the media file rather than a sidecar file. */
    val embedded: Boolean = false,
) {
    val isExternal: Boolean get() = !embedded
}

/**
 * Builds the track list from the sidecar names found next to the media and the text
 * tracks already inside it, de-duplicated and ordered: sidecars first, then embedded
 * tracks, both in the order given.
 */
fun buildSubtitleTracks(
    mediaName: String,
    sidecarNames: List<String>,
    embedded: List<SubtitleTrack>,
): List<SubtitleTrack> {
    val seen = mutableSetOf<String>()
    val tracks = mutableListOf<SubtitleTrack>()

    sidecarNames.forEach { name ->
        val mime = sidecarMimeType(name.substringAfterLast('.', "")) ?: return@forEach
        if (!sidecarMatchesMedia(mediaName, name)) return@forEach
        if (!seen.add(name.lowercase())) return@forEach
        tracks += SubtitleTrack(
            id = name,
            label = sidecarLabel(mediaName, name),
            language = sidecarLanguage(name),
            mimeType = mime,
        )
    }

    embedded.forEach { track ->
        if (seen.add(track.id.lowercase())) tracks += track
    }
    return tracks
}
