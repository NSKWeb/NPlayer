package com.example.nplayer.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the platform [Equalizer] for the player's audio session. The effect is
 * created against the session id of the running player, so it applies to local
 * playback and nothing else. Every call is defensive: the equalizer is optional
 * hardware, and constructing it throws on devices that lack it.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var minLevelMb = EQUALIZER_DEFAULT_MIN_MB
    private var maxLevelMb = EQUALIZER_DEFAULT_MAX_MB

    /** Binds to [audioSessionId], replacing any previous session. */
    fun attach(audioSessionId: Int) {
        if (equalizer != null) return
        val effect = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        if (effect == null) {
            _state.value = EqualizerState(available = false)
            return
        }
        equalizer = effect
        val range = effect.bandLevelRange
        minLevelMb = range[0].toInt()
        maxLevelMb = range[1].toInt()
        val names = (0 until effect.numberOfPresets.toInt()).map { effect.getPresetName(it.toShort()) }
        val labels = (0 until effect.numberOfBands.toInt()).map {
            formatBandFrequency(effect.getCenterFreq(it.toShort()) / 1000)
        }
        _state.value = EqualizerState(
            available = true,
            enabled = effect.enabled,
            // A fresh equalizer reports Normal as preset 0 and no custom preset.
            presetIndex = 0,
            presetNames = names,
            bandLevels = (0 until effect.numberOfBands.toInt()).map { effect.getBandLevel(it.toShort()).toInt() },
            bandLabels = labels,
        )
    }

    fun setEnabled(enabled: Boolean) {
        val effect = equalizer ?: return
        runCatching { effect.enabled = enabled }
        _state.value = _state.value.copy(enabled = enabled)
    }

    /** Selects a factory preset; band levels are re-read from the device. */
    fun setPreset(index: Int) {
        val effect = equalizer ?: return
        runCatching { effect.usePreset(index.toShort()) }
        _state.value = _state.value.copy(
            presetIndex = index,
            bandLevels = (0 until effect.numberOfBands.toInt()).map {
                effect.getBandLevel(it.toShort()).toInt()
            },
        )
    }

    /** Sets one band, which moves the equalizer off any factory preset. */
    fun setBandLevel(band: Int, levelMb: Int) {
        val effect = equalizer ?: return
        val clamped = clampLevel(levelMb, minLevelMb, maxLevelMb)
        runCatching { effect.setBandLevel(band.toShort(), clamped.toShort()) }
        val levels = _state.value.bandLevels.toMutableList()
        if (band in levels.indices) levels[band] = clamped
        _state.value = _state.value.copy(presetIndex = -1, bandLevels = levels)
    }

    fun levelRange(): IntRange = minLevelMb..maxLevelMb

    /** Re-reads the device levels, so the UI can mirror what actually applied. */
    fun refresh() {
        val effect = equalizer ?: return
        _state.value = _state.value.copy(
            enabled = effect.enabled,
            bandLevels = (0 until effect.numberOfBands.toInt()).map {
                effect.getBandLevel(it.toShort()).toInt()
            },
        )
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        _state.value = EqualizerState()
    }
}
