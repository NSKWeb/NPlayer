package com.example.nplayer.playback

import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout

/** How the video is fitted into the viewport. Maps onto Media3's resize modes. */
enum class ResizeMode(val label: String, val media3Mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ;

    fun next(): ResizeMode = entries[(ordinal + 1) % entries.size]
}

/** Media3 repeat modes, wrapped so the UI can cycle them without importing Media3. */
enum class RepeatMode(val label: String, val media3Mode: Int) {
    OFF("Repeat off", Player.REPEAT_MODE_OFF),
    ALL("Repeat all", Player.REPEAT_MODE_ALL),
    ONE("Repeat one", Player.REPEAT_MODE_ONE),
    ;

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}

/** An A–B loop. [isActive] only when both ends are set and B is after A. */
data class AbRepeat(val startMillis: Long? = null, val endMillis: Long? = null) {
    val isActive: Boolean
        get() = startMillis != null && endMillis != null && endMillis > startMillis

    val hasStart: Boolean get() = startMillis != null
}

/** Playback speed presets offered in the speed sheet. */
val SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)

/** Sleep-timer presets, in minutes. */
val SLEEP_TIMER_PRESETS = listOf(5, 10, 15, 30, 45, 60, 90)

/**
 * Next speed above [current], or [current] itself at the top of the range. Speeds
 * between presets (from a fine-grained control) snap up to the next preset.
 */
fun nextSpeed(current: Float): Float =
    SPEED_PRESETS.firstOrNull { it > current + 0.001f } ?: SPEED_PRESETS.last()

/** Previous speed below [current], or [current] itself at the bottom of the range. */
fun previousSpeed(current: Float): Float =
    SPEED_PRESETS.lastOrNull { it < current - 0.001f } ?: SPEED_PRESETS.first()

/** Renders a speed as "1x" or "1.5x", trimming a trailing ".0". */
fun formatSpeed(speed: Float): String {
    val rounded = (Math.round(speed * 100) / 100.0).toString()
    val trimmed = if (rounded.endsWith(".0")) rounded.dropLast(2) else rounded
    return "${trimmed}x"
}

/** Formats a position as "h:mm:ss" past an hour, otherwise "m:ss". */
fun formatClock(millis: Long): String {
    val safe = millis.coerceAtLeast(0)
    val totalSeconds = safe / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Formats a remaining sleep-timer duration as "45:00" or "1:05:00". */
fun formatSleepRemaining(remainingMillis: Long): String =
    formatClock(remainingMillis.coerceAtLeast(0))
