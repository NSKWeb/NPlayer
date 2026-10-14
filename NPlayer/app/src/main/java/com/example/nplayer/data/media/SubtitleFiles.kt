package com.example.nplayer.data.media

/**
 * The sidecar extension a Media3 extractor will accept for each subtitle format.
 * `.sub` is deliberately absent: it is usually MicroDVD, which the extractor cannot
 * parse, so offering it would promise subtitles the player cannot render.
 */
private val SIDECAR_MIME_TYPES = mapOf(
    "srt" to "application/x-subrip",
    "vtt" to "text/vtt",
    "webvtt" to "text/vtt",
    "ssa" to "text/x-ssa",
    "ass" to "text/x-ssa",
    "ttml" to "application/ttml+xml",
    "dfxp" to "application/ttml+xml",
    "xml" to "application/ttml+xml",
)

/** Extensions that are always a subtitle, even without a matching media name. */
private val SUBTITLE_EXTENSIONS: Set<String> get() = SIDECAR_MIME_TYPES.keys

/** Common two-letter and three-letter subtitle language tags, for a readable label. */
private val LANGUAGE_NAMES = mapOf(
    "en" to "English", "eng" to "English",
    "es" to "Spanish", "spa" to "Spanish",
    "fr" to "French", "fre" to "French", "fra" to "French",
    "de" to "German", "ger" to "German", "deu" to "German",
    "it" to "Italian", "ita" to "Italian",
    "pt" to "Portuguese", "por" to "Portuguese",
    "nl" to "Dutch", "nld" to "Dutch",
    "ru" to "Russian", "rus" to "Russian",
    "ar" to "Arabic", "ara" to "Arabic",
    "hi" to "Hindi", "hin" to "Hindi",
    "ja" to "Japanese", "jpn" to "Japanese",
    "ko" to "Korean", "kor" to "Korean",
    "zh" to "Chinese", "chi" to "Chinese", "zho" to "Chinese",
)

/** Turns "en" or "eng" into "English"; an unknown tag is returned uppercased. */
fun languageDisplayName(tag: String?): String? {
    val key = tag?.trim()?.lowercase()?.substringBefore('-') ?: return null
    if (key.isEmpty() || key == "und") return null
    return LANGUAGE_NAMES[key] ?: key.uppercase()
}

/**
 * The language a file-name tag names, or null. Unlike [languageDisplayName] this only
 * accepts a code it recognises, because a file name also carries release tags such as
 * `2024` or `1080p` that are not languages.
 */
private fun knownLanguageName(tag: String?): String? =
    LANGUAGE_NAMES[tag?.trim()?.lowercase()?.substringBefore('-')]

/** True when [extension] (without the dot) is a subtitle format Media3 can read. */
fun isSubtitleExtension(extension: String): Boolean =
    extension.lowercase() in SUBTITLE_EXTENSIONS

/** The MIME type Media3 needs for a sidecar with [extension], or null if unsupported. */
fun sidecarMimeType(extension: String): String? =
    SIDECAR_MIME_TYPES[extension.lowercase()]

/**
 * Whether a sidecar file called [candidateName] belongs to a media file called
 * [mediaName]. Both are file names, not paths.
 *
 * Matching is on the stem, so `Trip.2024.en.srt` matches `Trip.2024.mp4`, and on the
 * exact name, so `Movie.en.srt` matches `Movie.mkv`. A sidecar with a stem that has
 * nothing to do with the media is rejected, which keeps a folder full of unrelated
 * subtitle files from attaching the wrong one.
 */
fun sidecarMatchesMedia(mediaName: String, candidateName: String): Boolean {
    val mediaStem = mediaName.substringBeforeLast('.', mediaName).lowercase()
    val candidateStem = candidateName.substringBeforeLast('.', candidateName).lowercase()
    if (mediaStem.isEmpty() || candidateStem.isEmpty()) return false
    if (candidateStem == mediaStem) return true
    // A language or release tag between the stem and the extension, e.g. "Movie.en".
    return candidateStem.startsWith("$mediaStem.") || candidateStem.startsWith("${mediaStem}_")
}

/** The last dot-separated tag of a file name's stem, e.g. "en" in "Trip.2024.en.srt". */
private fun trailingTag(candidateName: String): String =
    candidateName.substringBeforeLast('.', candidateName).substringAfterLast('.', "")

/** The language a sidecar name names, e.g. "English" for "Trip.2024.en.srt", if any. */
fun sidecarLanguage(candidateName: String): String? =
    knownLanguageName(trailingTag(candidateName))

/**
 * The ISO tag embedded in a sidecar name, kept as a tag so it can be handed to Media3
 * as `language` rather than as a display name.
 */
fun sidecarLanguageTag(candidateName: String): String? =
    trailingTag(candidateName).takeIf { knownLanguageName(it) != null }?.lowercase()

/**
 * The label shown for a sidecar file, e.g. "Trip.2024.en.srt" becomes "English" when
 * a language is recognised, otherwise the file name with its extension stripped.
 */
fun sidecarLabel(mediaName: String, candidateName: String): String {
    sidecarLanguage(candidateName)?.let { return it }
    val stem = candidateName.substringBeforeLast('.', candidateName)
    val mediaStem = mediaName.substringBeforeLast('.', mediaName)
    return stem.removePrefix(mediaStem).trim('.', '_', '-', ' ').ifBlank { "Subtitles" }
}
