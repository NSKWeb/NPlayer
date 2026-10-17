package com.example.nplayer.playback

/**
 * The position a relative seek should land on, clamped to the media bounds.
 *
 * A non-positive [durationMillis] means the duration is not known yet, so only the
 * lower bound applies and a forward jump is left alone rather than clipped to 0.
 */
fun seekTarget(currentPositionMillis: Long, deltaMillis: Long, durationMillis: Long): Long {
    val upper = if (durationMillis > 0) durationMillis else Long.MAX_VALUE
    return (currentPositionMillis + deltaMillis).coerceIn(0, upper)
}

/**
 * How far a horizontal drag covering [fraction] of the video's width should move
 * playback: dragging the whole width covers the whole file.
 *
 * Returns 0 when the duration is unknown, which is the state the player is in for the
 * first moments after a video opens.
 */
fun dragSeekDelta(fraction: Float, durationMillis: Long): Long {
    if (durationMillis <= 0) return 0
    return (fraction * durationMillis).toLong()
}
