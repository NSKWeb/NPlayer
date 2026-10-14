package com.example.nplayer.playback

/**
 * A one-tap seek jump offered in the player's quick-jump row. [label] is what the
 * user sees; [deltaMillis] is signed, so a negative value rewinds.
 */
data class SeekStep(val label: String, val deltaMillis: Long) {
    val isForward: Boolean get() = deltaMillis > 0
}

/**
 * Long jumps, in the order they are shown. These are the "20 / 40 minute" style
 * hops that make a long film navigable without dragging the scrubber.
 */
val QUICK_SEEK_STEPS = listOf(
    SeekStep("+20m", 20 * 60_000L),
    SeekStep("+40m", 40 * 60_000L),
    SeekStep("−20m", -20 * 60_000L),
    SeekStep("−40m", -40 * 60_000L),
)

/**
 * Moves [positionMillis] by [deltaMillis], clamped to the media bounds so a jump
 * never seeks past the end or before the start. An unknown duration (0) is treated
 * as open-ended forward, matching how the player resolves it.
 */
fun seekTarget(positionMillis: Long, deltaMillis: Long, durationMillis: Long): Long {
    val upper = if (durationMillis > 0) durationMillis else Long.MAX_VALUE
    return (positionMillis + deltaMillis).coerceIn(0L, upper)
}
