package com.example.nplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.data.playlists.PlaylistStore
import com.example.nplayer.playback.AbRepeat
import com.example.nplayer.playback.EqualizerController
import com.example.nplayer.playback.EqualizerState
import com.example.nplayer.playback.PlaybackController
import com.example.nplayer.playback.RepeatMode
import com.example.nplayer.playback.ResizeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
data class PlayerUiState(
    val player: androidx.media3.common.Player? = null,
    val controller: MediaController? = null,
    val title: String? = null,
    val error: String? = null,
    val speed: Float = 1f,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val mirrored: Boolean = false,
    val nightMode: Boolean = false,
    val locked: Boolean = false,
    val abRepeat: AbRepeat = AbRepeat(),
    val sleepRemainingMillis: Long? = null,
    val brightness: Float = 0.5f,
    val volume: Float = 1f,
)

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val playbackController: PlaybackController,
    private val playlistStore: PlaylistStore,
    private val equalizerController: EqualizerController,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _equalizer = MutableStateFlow(EqualizerState())
    val equalizer: StateFlow<EqualizerState> = _equalizer.asStateFlow()

    private var abLoopJob: Job? = null
    private var sleepJob: Job? = null
    private var equalizerJob: Job? = null

    /** [queuePlaylistId] plays the whole playlist as a queue instead of one item. */
    fun onEnter(itemKey: String, queuePlaylistId: String? = null) {
        viewModelScope.launch {
            val controller = playbackController.connectAndAwait()
            _state.update {
                it.copy(
                    player = controller,
                    controller = controller,
                    speed = controller.playbackParameters.speed,
                    repeatMode = repeatModeOf(controller.repeatMode),
                    shuffle = controller.shuffleModeEnabled,
                )
            }

            library.refreshIfEmpty()
            val item = library.find(itemKey)
            if (item == null) {
                _state.update { it.copy(error = "That file is no longer available") }
                return@launch
            }
            _state.update { it.copy(title = item.title) }

            // A playlist resolves to the items that still exist, in playlist order.
            // Hidden items are left out of the queue, so playing a playlist cannot
            // surface a private file.
            val queue = queuePlaylistId
                ?.let { id -> playlistStore.current().firstOrNull { it.id == id } }
                ?.itemKeys
                ?.mapNotNull { library.findVisible(it) }
                .orEmpty()

            val startIndex = queue.indexOfFirst { it.key == item.key }
            if (queue.isNotEmpty() && startIndex >= 0) {
                playbackController.playQueue(queue, startIndex)
            } else {
                playbackController.play(item)
            }

            // The service attaches the equalizer once the audio session exists;
            // this just mirrors the controller's state into the UI.
            mirrorEqualizer()
        }
    }

    private fun mirrorEqualizer() {
        equalizerJob?.cancel()
        equalizerJob = viewModelScope.launch {
            equalizerController.state.collect { _equalizer.value = it }
        }
    }

    fun onLeave(itemKey: String) {
        viewModelScope.launch { playbackController.stop(itemKey) }
        abLoopJob?.cancel()
        sleepJob?.cancel()
        equalizerJob?.cancel()
        playbackController.release()
    }

    // --- Equalizer ---------------------------------------------------------

    fun setEqualizerEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)

    fun setEqualizerPreset(index: Int) = equalizerController.setPreset(index)

    fun setEqualizerBand(band: Int, levelMb: Int) = equalizerController.setBandLevel(band, levelMb)

    fun equalizerRange(): IntRange = equalizerController.levelRange()

    private val player: Player? get() = _state.value.controller

    // --- Playback controls -------------------------------------------------

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekBy(deltaMillis: Long) {
        val p = player ?: return
        val duration = if (p.duration > 0) p.duration else Long.MAX_VALUE
        val target = (p.currentPosition + deltaMillis).coerceIn(0, duration)
        p.seekTo(target)
    }

    fun seekTo(positionMillis: Long) {
        player?.seekTo(positionMillis.coerceAtLeast(0))
    }

    fun setSpeed(speed: Float) {
        val p = player ?: return
        p.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    fun cycleResizeMode() = _state.update { it.copy(resizeMode = it.resizeMode.next()) }

    fun cycleRepeatMode() {
        val p = player ?: return
        val next = _state.value.repeatMode.next()
        p.repeatMode = next.media3Mode
        _state.update { it.copy(repeatMode = next) }
    }

    fun toggleShuffle() {
        val p = player ?: return
        val next = !p.shuffleModeEnabled
        p.shuffleModeEnabled = next
        _state.update { it.copy(shuffle = next) }
    }

    fun toggleMirror() = _state.update { it.copy(mirrored = !it.mirrored) }

    fun toggleNightMode() = _state.update { it.copy(nightMode = !it.nightMode) }

    fun toggleLock() = _state.update { it.copy(locked = !it.locked) }

    /** Attaches a user-picked subtitle file to the current item. */
    fun attachSubtitle(uri: String) = playbackController.addSubtitle(uri)

    // --- Gesture-driven adjustments ---------------------------------------

    /** Applies a brightness delta from a vertical gesture, clamped to 0..1. */
    fun adjustBrightness(delta: Float) =
        _state.update { it.copy(brightness = (it.brightness + delta).coerceIn(0f, 1f)) }

    /** Applies a volume delta to the player, clamped to 0..1. */
    fun adjustVolume(delta: Float) {
        val p = player ?: return
        val next = (p.volume + delta).coerceIn(0f, 1f)
        p.volume = next
        _state.update { it.copy(volume = next) }
    }

    // --- A–B repeat --------------------------------------------------------

    /** Sets A on the first press and B on the second, then loops between them. */
    fun markAbRepeat() {
        val p = player ?: return
        val now = p.currentPosition
        val current = _state.value.abRepeat
        when {
            !current.hasStart -> {
                _state.update { it.copy(abRepeat = AbRepeat(startMillis = now)) }
            }
            !current.isActive -> {
                val updated = current.copy(endMillis = now)
                _state.update { it.copy(abRepeat = updated) }
                if (updated.isActive) startAbLoop()
            }
            else -> {
                abLoopJob?.cancel()
                _state.update { it.copy(abRepeat = AbRepeat()) }
            }
        }
    }

    private fun startAbLoop() {
        abLoopJob?.cancel()
        abLoopJob = viewModelScope.launch {
            while (isActive) {
                val p = player ?: break
                val ab = _state.value.abRepeat
                if (!ab.isActive) break
                if (p.currentPosition >= ab.endMillis!!) p.seekTo(ab.startMillis!!)
                delay(200)
            }
        }
    }

    // --- Sleep timer -------------------------------------------------------

    /** Starts a timer that pauses playback after [minutes]; null cancels it. */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            _state.update { it.copy(sleepRemainingMillis = null) }
            return
        }
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60_000L
            while (isActive && remaining > 0) {
                _state.update { it.copy(sleepRemainingMillis = remaining) }
                delay(1_000)
                remaining -= 1_000
            }
            if (isActive) {
                player?.pause()
                _state.update { it.copy(sleepRemainingMillis = null) }
            }
        }
    }

    private fun repeatModeOf(media3Mode: Int): RepeatMode =
        RepeatMode.entries.firstOrNull { it.media3Mode == media3Mode } ?: RepeatMode.OFF
}
