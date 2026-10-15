package com.example.nplayer.data.lyrics

import com.example.nplayer.data.media.sidecarMatchesMedia

/** Extensions that are a lyrics file. LRC is the timed format the app can sync to. */
private val LYRICS_EXTENSIONS = setOf("lrc")

/** True when [extension] (without the dot) is a timed lyrics format. */
fun isLyricsExtension(extension: String): Boolean =
    extension.lowercase() in LYRICS_EXTENSIONS

/**
 * Whether a lyrics file called [candidateName] belongs to an audio file called
 * [mediaName]. The stem rule is the one sidecar subtitles already use, so `Song.lrc`
 * matches `Song.mp3` and `Song.en.lrc` matches `Song.flac`.
 */
fun lyricsMatchesMedia(mediaName: String, candidateName: String): Boolean =
    isLyricsExtension(candidateName.substringAfterLast('.', "")) &&
        sidecarMatchesMedia(mediaName, candidateName)

/**
 * The best lyrics file for [mediaName] out of [candidates], or null. An exact stem match
 * (`Song.lrc`) beats a tagged one (`Song.en.lrc`), and ties are broken by name so the
 * choice is stable across scans.
 */
fun pickLyricsFile(mediaName: String, candidates: List<String>): String? {
    val matches = candidates.filter { lyricsMatchesMedia(mediaName, it) }
    if (matches.isEmpty()) return null
    val mediaStem = mediaName.substringBeforeLast('.', mediaName).lowercase()
    return matches.sortedWith(
        compareBy(
            { it.substringBeforeLast('.', it).lowercase() != mediaStem },
            { it.lowercase() },
        ),
    ).first()
}
