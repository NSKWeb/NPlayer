package com.example.nplayer.playback

import kotlin.math.roundToInt

/**
 * Equalizer levels are in millibels. Android's own range is usually ±1500 mB, but it
 * is device-dependent, so [clampLevel] keeps a value inside whatever range is reported.
 */
const val EQUALIZER_DEFAULT_MIN_MB = -1500
const val EQUALIZER_DEFAULT_MAX_MB = 1500

/** The equalizer's live state, mirrored into the UI. */
data class EqualizerState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val presetIndex: Int = 0,
    val presetNames: List<String> = emptyList(),
    val bandLevels: List<Int> = emptyList(),
    val bandLabels: List<String> = emptyList(),
) {
    val isCustom: Boolean get() = presetIndex < 0
}

fun clampLevel(levelMb: Int, minMb: Int, maxMb: Int): Int = levelMb.coerceIn(minMb, maxMb)

/** Maps a slider fraction (0..1) to a millibel level across the device's range. */
fun levelFromFraction(fraction: Float, minMb: Int, maxMb: Int): Int =
    (minMb + (maxMb - minMb) * fraction.coerceIn(0f, 1f)).roundToInt()

/** Maps a millibel level back to a slider fraction (0..1). */
fun fractionFromLevel(levelMb: Int, minMb: Int, maxMb: Int): Float {
    if (maxMb <= minMb) return 0.5f
    return ((levelMb - minMb).toFloat() / (maxMb - minMb)).coerceIn(0f, 1f)
}

/** Human label for a band, e.g. 60 Hz, 1 kHz, 16 kHz. */
fun formatBandFrequency(hertz: Int): String = when {
    hertz >= 1000 -> "${(hertz / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it) }} kHz"
    else -> "$hertz Hz"
}

/** True when the band levels match [presetLevels] within [toleranceMb]. */
fun matchesPreset(levels: List<Int>, presetLevels: List<Int>, toleranceMb: Int = 0): Boolean {
    if (levels.size != presetLevels.size) return false
    return levels.zip(presetLevels).all { (a, b) -> kotlin.math.abs(a - b) <= toleranceMb }
}
