package com.example.nplayer.data.lyrics

/** One timed line of a lyrics file. [text] is plain, with any inline timestamps removed. */
data class LyricLine(val timeMillis: Long, val text: String)

/** A parsed lyrics file: the lines in time order, plus any `[ti:]`/`[ar:]` metadata. */
data class Lyrics(
    val lines: List<LyricLine> = emptyList(),
    val title: String? = null,
    val artist: String? = null,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

/**
 * Matches a `[mm:ss.xx]` timestamp. Hundredths are optional and one to three digits are
 * accepted, because files in the wild write both `[01:02.5]` and `[01:02.500]`.
 */
private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/**
 * Parses LRC (and the timestamped subset of enhanced LRC) into timed lines.
 *
 * A line may carry several timestamps, meaning the same words repeat at each time; each
 * one becomes its own entry. Lines with no timestamp, such as `[ti:Title]`, are read as
 * metadata, and anything unrecognised is skipped rather than failing the whole file.
 */
fun parseLrc(source: String): Lyrics {
    val lines = mutableListOf<LyricLine>()
    var title: String? = null
    var artist: String? = null

    source.lineSequence().forEach { raw ->
        val matches = TIMESTAMP.findAll(raw).toList()
        if (matches.isEmpty()) {
            readMetadata(raw)?.let { (key, value) ->
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                }
            }
            return@forEach
        }

        // Everything after the last timestamp is the words; inline word timestamps
        // (enhanced LRC) are stripped so only the lyric text is kept.
        val text = raw.substring(matches.last().range.last + 1)
            .replace(Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>"""), "")
            .trim()

        matches.forEach { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
            val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
            val fraction = match.groupValues[3]
            val millis = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.take(3).toLong()
            }
            lines += LyricLine(minutes * 60_000 + seconds * 1_000 + millis, text)
        }
    }

    return Lyrics(lines.sortedBy { it.timeMillis }, title, artist)
}

/** Reads a `[key:value]` metadata tag, lower-casing the key for comparison. */
private fun readMetadata(raw: String): Pair<String, String>? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null
    val body = trimmed.substring(1, trimmed.length - 1)
    val separator = body.indexOf(':')
    if (separator <= 0) return null
    val key = body.substring(0, separator).trim().lowercase()
    val value = body.substring(separator + 1).trim()
    if (value.isEmpty()) return null
    return key to value
}

/**
 * The index of the line playing at [positionMillis]: the last line whose time is at or
 * before it, or -1 before the first line. [lines] must be sorted by time.
 */
fun activeLyricIndex(lines: List<LyricLine>, positionMillis: Long): Int {
    var low = 0
    var high = lines.size - 1
    var result = -1
    while (low <= high) {
        val mid = (low + high) / 2
        if (lines[mid].timeMillis <= positionMillis) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return result
}
