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

/** The span a full-width swipe covers when the duration is not known: 90 seconds. */
const val DRAG_SEEK_FALLBACK_MILLIS = 90_000L

/**
 * How far a horizontal drag covering [fraction] of the video's width should move
 * playback: dragging the whole width covers the whole file.
 *
 * When the duration is unknown the swipe still has to move something. Media3 reports
 * [androidx.media3.common.C.TIME_UNSET] until the item is prepared, and for some
 * containers never reports it at all, so scaling by it yielded a delta of 0 and the
 * slide did nothing. A fixed [DRAG_SEEK_FALLBACK_MILLIS] span is used instead, which
 * keeps the gesture working whatever the player knows.
 */
fun dragSeekDelta(fraction: Float, durationMillis: Long): Long {
    val span = if (durationMillis > 0) durationMillis else DRAG_SEEK_FALLBACK_MILLIS
    return (fraction * span).toLong()
}
