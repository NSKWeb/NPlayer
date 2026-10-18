package com.example.nplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.nplayer.data.lyrics.Lyrics
import com.example.nplayer.data.lyrics.LyricsRepository
import com.example.nplayer.data.lyrics.activeLyricIndex
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.playlists.PlaylistStore
import com.example.nplayer.data.prefs.PlaybackSettings
import com.example.nplayer.data.prefs.PlaybackSettingsStore
import com.example.nplayer.data.prefs.RecentlyPlayedStore
import com.example.nplayer.data.prefs.ResumeMode
import com.example.nplayer.playback.AbRepeat
import com.example.nplayer.playback.AudioTrack
import com.example.nplayer.playback.EqualizerController
import com.example.nplayer.playback.EqualizerState
import com.example.nplayer.playback.PlaybackController
import com.example.nplayer.playback.RepeatMode
import com.example.nplayer.playback.ResizeMode
import com.example.nplayer.playback.SubtitleTrack
import com.example.nplayer.playback.buildQueue
import com.example.nplayer.playback.queueIndexOf
import com.example.nplayer.playback.seekTarget
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
    /** True when the queue has an item after this one, so "next" can be enabled. */
    val hasNext: Boolean = false,
    /** True when the queue has an item before this one, so "previous" can be enabled. */
    val hasPrevious: Boolean = false,
    /** The queue being played, in order; used to show what is coming up next. */
    val queue: List<MediaItem> = emptyList(),
    /** The queue item playing now, or null when nothing is queued. */
    val currentItem: MediaItem? = null,
    val brightness: Float = 0.5f,
    val volume: Float = 1f,
    /** Every subtitle track on the current item: sidecar files, then embedded ones. */
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    /** The track being shown, or null when subtitles are off. */
    val activeSubtitle: SubtitleTrack? = null,
    /** The caption line to draw over the video, already stripped of styling tags. */
    val subtitleText: String? = null,
    /** Timed lyrics for the audio track, when a matching sidecar was found. */
    val lyrics: Lyrics = Lyrics(),
    /** Index into [lyrics] lines for the position playing now, or -1 before the first. */
    val activeLyric: Int = -1,
    /** Every audio track in the current item, so one can be chosen over another. */
    val audioTracks: List<AudioTrack> = emptyList(),
    /** The saved position of the item being opened, when one exists. */
    val savedPositionMillis: Long = 0,
    /** Playback preferences in force for this session, so the UI can honour them. */
    val prefs: PlaybackSettings = PlaybackSettings(),
)

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val playbackController: PlaybackController,
    private val playlistStore: PlaylistStore,
    private val equalizerController: EqualizerController,
    private val lyricsRepository: LyricsRepository,
    private val playbackSettings: PlaybackSettingsStore,
    private val recentlyPlayed: RecentlyPlayedStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _equalizer = MutableStateFlow(EqualizerState())
    val equalizer: StateFlow<EqualizerState> = _equalizer.asStateFlow()

    private var abLoopJob: Job? = null
    private var sleepJob: Job? = null
    private var equalizerJob: Job? = null
    private var subtitleJob: Job? = null
    private var transportJob: Job? = null
    private var lyricsJob: Job? = null
    private var resumeSeekJob: Job? = null

    /** Set when the viewer turns subtitles off, so auto-select does not turn them back on. */
    private var subtitlesUserDisabled = false

    /**
     * True while the viewer is playing through a folder. Next/previous are driven by
     * the queue, so the buttons only appear once there is somewhere to go.
     */
    private var playingFromFolder = false

    /** [queuePlaylistId] plays the whole playlist as a queue instead of one item. */
    fun onEnter(itemKey: String, queuePlaylistId: String? = null) {
        subtitlesUserDisabled = false
        viewModelScope.launch {
            val controller = playbackController.connectAndAwait()
            val prefs = playbackSettings.current()
            _state.update {
                it.copy(
                    player = controller,
                    controller = controller,
                    speed = if (prefs.rememberSpeed) prefs.lastSpeed else controller.playbackParameters.speed,
                    resizeMode = if (prefs.rememberAspectRatio) prefs.lastResizeMode else ResizeMode.FIT,
                    brightness = if (prefs.rememberBrightness) prefs.lastBrightness else 0.5f,
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
            // The resume setting decides whether a saved position is used: "always"
            // seeks straight to it, "never" starts over, and "ask" leaves the choice
            // to the Resume button, so playback begins at the top.
            val saved = savedPosition(itemKey)
            val resume = when (prefs.resumeMode) {
                ResumeMode.ALWAYS -> true
                ResumeMode.NEVER -> false
                ResumeMode.ASK -> false
            }
            _state.update { it.copy(savedPositionMillis = saved, prefs = prefs) }
            // "Remember speed" carries the last speed into this item; otherwise the
            // controller's own default stands.
            if (prefs.rememberSpeed) controller.setPlaybackSpeed(prefs.lastSpeed)
            val playingQueue: List<MediaItem>
            when {
                queue.isNotEmpty() && startIndex >= 0 -> {
                    playingFromFolder = false
                    playingQueue = queue
                    playbackController.playQueue(
                        queue,
                        startIndex,
                        sidecars = { library.sidecarsFor(it) },
                        resume = resume,
                    )
                }
                else -> {
                    // Opened on its own: play the rest of its folder on from it, so the
                    // next/previous buttons and auto-advance have somewhere to go.
                    val folderQueue = buildQueue(library.videos() + library.audio(), item.key, item.kind)
                    playingFromFolder = folderQueue.isNotEmpty()
                    playingQueue = folderQueue
                    if (folderQueue.isNotEmpty()) {
                        playbackController.playQueue(
                            folderQueue,
                            queueIndexOf(folderQueue, item.key),
                            sidecars = { library.sidecarsFor(it) },
                            resume = resume,
                        )
                    } else {
                        playbackController.play(
                            item,
                            sidecars = library.sidecarsFor(item),
                            resume = resume,
                        )
                    }
                }
            }
            _state.update {
                it.copy(queue = playingQueue, currentItem = item)
            }

            // The resume position is applied once the timeline is ready rather than
            // being handed to the controller as a start position: a seek issued before
            // the container has been read is dropped, which is how "always resume"
            // silently started from the beginning.
            if (resume && saved > 0) applySeekWhenReady(saved)

            // Remember that this item was played, so Home can offer a way back to it.
            recentlyPlayed.record(item.key)

            // The service attaches the equalizer once the audio session exists;
            // this just mirrors the controller's state into the UI.
            mirrorEqualizer()
            refreshSubtitles()
            mirrorSubtitles()
            mirrorTransport()
            loadLyrics(item)
            refreshAudioTracks()
        }
    }

    /** The saved position of [itemKey], or 0 when there is none. */
    suspend fun savedPosition(itemKey: String): Long = playbackController.savedPosition(itemKey)

    /** Seeks to the position saved for the item playing now. */
    fun resumeSavedPosition() {
        val saved = _state.value.savedPositionMillis
        if (saved > 0) applySeekWhenReady(saved)
    }

    /**
     * Applies [positionMillis] as soon as the player knows the duration.
     *
     * Media3 discards a seek issued before the timeline is ready, and the moment an item
     * opens is exactly that window — `duration` is still unset while the container is
     * being read. A resume issued then is silently lost, so the position is retried until
     * it can actually be applied.
     */
    private fun applySeekWhenReady(positionMillis: Long) {
        resumeSeekJob?.cancel()
        resumeSeekJob = viewModelScope.launch {
            // Retries until the duration appears, which for a large file can take far
            // longer than any fixed budget: a 2 GB MKV may not report one for half a
            // minute, and a bounded loop silently gave up and started from 0:00. The
            // job lives with the view model, so it is cancelled with the screen and
            // cannot outlive it.
            while (isActive) {
                val p = player ?: return@launch
                if (p.duration > 0) {
                    p.seekTo(positionMillis.coerceIn(0, p.duration))
                    return@launch
                }
                delay(100)
            }
        }
    }

    /** Re-reads the audio tracks of the current item, e.g. after the queue advanced. */
    private fun refreshAudioTracks() {
        val tracks = playbackController.audioTracks()
        if (tracks != _state.value.audioTracks) {
            _state.update { it.copy(audioTracks = tracks) }
            applyPreferredAudioTrack(tracks)
        }
    }

    /**
     * Switches to the track matching the preferred audio language, once per item, and
     * only when the container offers one. Without a preference, or with no match, the
     * file's own default track stays.
     */
    private fun applyPreferredAudioTrack(tracks: List<AudioTrack>) {
        val preferred = _state.value.prefs.preferredAudioLanguage ?: return
        if (tracks.size < 2) return
        if (tracks.any { it.selected && it.language?.startsWith(preferred) == true }) return
        val match = tracks.firstOrNull { it.language?.startsWith(preferred) == true } ?: return
        if (match.selected) return
        playbackController.selectAudioTrack(match)
    }

    /** Plays [track] instead of the audio track playing now. */
    fun selectAudioTrack(track: AudioTrack) {
        playbackController.selectAudioTrack(track)
        // The override takes effect on the next poll; read it back so the tick moves.
        viewModelScope.launch {
            delay(150)
            refreshAudioTracks()
        }
    }

    /**
     * Loads the timed lyrics sidecar for [item] and tracks the line playing now. Only
     * audio carries lyrics, and a missing file simply leaves the lyrics empty.
     */
    private fun loadLyrics(item: MediaItem) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            val file = library.lyricsFor(item)
            val lyrics = file?.let { lyricsRepository.load(it.uri) } ?: Lyrics()
            _state.update { it.copy(lyrics = lyrics, activeLyric = -1) }
            if (lyrics.isEmpty) return@launch
            while (isActive) {
                val p = player
                if (p != null) {
                    val index = activeLyricIndex(lyrics.lines, p.currentPosition)
                    if (index != _state.value.activeLyric) {
                        _state.update { it.copy(activeLyric = index) }
                    }
                }
                delay(250)
            }
        }
    }

    /**
     * Next/previous availability changes as the queue advances, and Media3 has no
     * callback that fires for every case, so it is polled alongside the position.
     */
    private fun mirrorTransport() {
        transportJob?.cancel()
        transportJob = viewModelScope.launch {
            while (isActive) {
                val p = player
                val hasNext = p?.hasNextMediaItem() == true
                val hasPrevious = p?.hasPreviousMediaItem() == true
                // As the queue advances the playing item changes, so the header title and
                // the "up next" highlight follow whichever item is playing now.
                val current = _state.value.queue.getOrNull(p?.currentMediaItemIndex ?: -1)
                if (hasNext != _state.value.hasNext ||
                    hasPrevious != _state.value.hasPrevious ||
                    current != _state.value.currentItem
                ) {
                    _state.update {
                        it.copy(
                            hasNext = hasNext,
                            hasPrevious = hasPrevious,
                            currentItem = current ?: it.currentItem,
                            title = current?.title ?: it.title,
                        )
                    }
                }
                delay(500)
            }
        }
    }

    /**
     * Subtitle tracks appear only after the player has read the file, and an embedded
     * track list arrives with the first `onTracksChanged`, so the list is re-read on a
     * short poll while the player settles.
     */
    private fun refreshSubtitles() {
        val tracks = playbackController.subtitleTracks()
        val active = playbackController.selectedSubtitleTrack()
        // A single sidecar is the common case, so it is shown without a tap — unless
        // the viewer has just turned subtitles off on purpose, or has turned the
        // "show subtitles by default" setting off.
        val auto = if (active == null && !subtitlesUserDisabled && _state.value.prefs.showSubtitlesByDefault) {
            val preferred = _state.value.prefs.preferredSubtitleLanguage
            val matching = preferred?.let { tag ->
                tracks.firstOrNull { it.language?.startsWith(tag) == true }
            }
            matching ?: tracks.singleOrNull { it.isExternal }
        } else {
            null
        }
        if (active == null && auto != null) playbackController.selectSubtitle(auto)
        _state.update {
            it.copy(
                subtitleTracks = tracks,
                activeSubtitle = playbackController.selectedSubtitleTrack() ?: auto,
            )
        }
    }

    /** Mirrors the caption text Media3 is currently rendering into the overlay. */
    private fun mirrorSubtitles() {
        subtitleJob?.cancel()
        subtitleJob = viewModelScope.launch {
            while (isActive) {
                val text = playbackController.currentCueText()
                if (text != _state.value.subtitleText) {
                    _state.update { it.copy(subtitleText = text) }
                }
                // The track list changes when the queue moves on, which can happen
                // without the count changing, so the ids are compared, not the size.
                val ids = playbackController.subtitleTracks().map { it.id }
                if (ids != _state.value.subtitleTracks.map { it.id }) refreshSubtitles()
                refreshAudioTracks()
                delay(400)
            }
        }
    }

    private fun mirrorEqualizer() {
        equalizerJob?.cancel()
        equalizerJob = viewModelScope.launch {
            equalizerController.state.collect { _equalizer.value = it }
        }
    }

    fun onLeave(itemKey: String) {
        // "Remember background play" keeps the audio going once the player screen
        // closes; the media service owns the player, so it survives this screen. The
        // position is still saved, so coming back resumes where the sound is.
        if (_state.value.prefs.rememberBackgroundPlay && player?.isPlaying == true) {
            playbackController.savePosition(itemKey)
            cancelPlayerJobs()
            _state.update { it.copy(player = null, controller = null) }
            return
        }
        playbackController.stop(itemKey)
        cancelPlayerJobs()
        playbackController.release()
    }

    /** Stops the polling jobs the player screen started. */
    private fun cancelPlayerJobs() {
        abLoopJob?.cancel()
        sleepJob?.cancel()
        equalizerJob?.cancel()
        subtitleJob?.cancel()
        transportJob?.cancel()
        lyricsJob?.cancel()
        resumeSeekJob?.cancel()
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

    /**
     * Moves to the next item in the queue. With repeat off at the end of a folder
     * queue there is nowhere to go, so the last item is left playing rather than
     * restarting the folder.
     */
    fun skipToNext() {
        val p = player ?: return
        if (p.hasNextMediaItem()) {
            cancelPendingResume()
            p.seekToNextMediaItem()
        }
    }

    /**
     * Moves to the previous item. Like the usual music-player behaviour, a few seconds
     * into the current item this restarts it instead, so a stray tap does not jump
     * back a whole file.
     */
    fun skipToPrevious() {
        val p = player ?: return
        cancelPendingResume()
        if (p.hasPreviousMediaItem() && p.currentPosition < 3_000) {
            p.seekToPreviousMediaItem()
        } else {
            p.seekTo(0)
        }
    }

    /**
     * Moves playback by [deltaMillis].
     *
     * The seek is issued straight away and never depends on the duration being known.
     * Media3 reports the duration as [androidx.media3.common.C.TIME_UNSET] until the item
     * is prepared — and for some containers, not at all. 1.3.0 refused to seek in that
     * window, so a press did nothing; clamping to an unset duration is worse, because it
     * collapses the target to 0 and jumps to the start. [seekTarget] applies the lower
     * bound always and the upper bound only once a length is known.
     *
     * Any pending resume is cancelled first. A resume is parked until the duration
     * arrives, and on a long file that can be several seconds into playback: without
     * this the parked resume fired after the press and dragged playback back to the
     * saved position, so the seek looked like it did nothing. Short files hid it,
     * because their duration arrives before the first press can land.
     */
    fun seekBy(deltaMillis: Long) {
        val p = player ?: return
        cancelPendingResume()
        p.seekTo(seekTarget(p.currentPosition, deltaMillis, p.duration))
    }

    /**
     * Seeks to an absolute [positionMillis]. Past-the-end positions are clamped only
     * once the duration is known, so a seek is never dropped for want of a length.
     * Like [seekBy], this supersedes a parked resume.
     */
    fun seekTo(positionMillis: Long) {
        val p = player ?: return
        cancelPendingResume()
        val target = if (p.duration > 0) {
            positionMillis.coerceIn(0, p.duration)
        } else {
            positionMillis.coerceAtLeast(0)
        }
        p.seekTo(target)
    }

    /**
     * Drops a resume that has not been applied yet, because the viewer has since
     * chosen a position of their own. The saved position stays on disk; only the
     * deferred seek is abandoned.
     */
    private fun cancelPendingResume() {
        resumeSeekJob?.cancel()
        resumeSeekJob = null
    }

    fun setSpeed(speed: Float) {
        val p = player ?: return
        p.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
        viewModelScope.launch { playbackSettings.rememberSpeed(speed) }
    }

    fun cycleResizeMode() {
        val next = _state.value.resizeMode.next()
        _state.update { it.copy(resizeMode = next) }
        viewModelScope.launch { playbackSettings.rememberResizeMode(next) }
    }

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

    // --- Subtitles ---------------------------------------------------------

    /**
     * Attaches a subtitle file the user picked by hand. It is appended to the item
     * that is playing and shown straight away.
     */
    fun attachSubtitle(uri: String) {
        playbackController.attachPickedSubtitle(uri)
        viewModelScope.launch {
            delay(300)
            refreshSubtitles()
        }
    }

    /** Shows [track], or turns subtitles off when it is null. */
    fun selectSubtitle(track: SubtitleTrack?) {
        playbackController.selectSubtitle(track)
        subtitlesUserDisabled = track == null
        _state.update { it.copy(activeSubtitle = track, subtitleText = null) }
    }

    // --- Gesture-driven adjustments ---------------------------------------

    /** Applies a brightness delta from a vertical gesture, clamped to 0..1. */
    fun adjustBrightness(delta: Float) {
        val next = (_state.value.brightness + delta).coerceIn(0f, 1f)
        _state.update { it.copy(brightness = next) }
        viewModelScope.launch { playbackSettings.rememberBrightness(next) }
    }

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
