package com.example.nplayer.ui.player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import com.example.nplayer.data.media.MediaKind
import com.example.nplayer.data.prefs.OrientationMode
import com.example.nplayer.data.prefs.OverlayItem
import com.example.nplayer.data.prefs.ResumeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.nplayer.MainActivity
import com.example.nplayer.playback.SLEEP_TIMER_PRESETS
import com.example.nplayer.playback.SPEED_PRESETS
import com.example.nplayer.playback.dragSeekDelta
import com.example.nplayer.playback.formatClock
import com.example.nplayer.playback.formatSleepRemaining
import com.example.nplayer.playback.formatSpeed
import kotlinx.coroutines.delay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextAlign
import com.example.nplayer.playback.AudioTrack
import com.example.nplayer.playback.SubtitleTrack

@UnstableApi
@Composable
fun PlayerScreen(
    itemKey: String,
    onBack: () -> Unit,
    queuePlaylistId: String? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    // Video and audio share one player and one view model; only the surface differs.
    // Video is the fullscreen `PlayerView`, audio is a music-player layout with no
    // video surface at all, so neither carries controls the other has no use for.
    val isVideo = state.currentItem?.kind != MediaKind.AUDIO

    DisposableEffect(Unit) {
        viewModel.onEnter(itemKey, queuePlaylistId)
        onDispose {
            viewModel.onLeave(itemKey)
            // Leaving the player must not leave the window locked sideways.
            activity?.resetOrientation()
        }
    }

    // Hide the status and navigation bars for the whole time the player is on screen,
    // and put them back on the way out. Swiping still reveals them briefly, which is
    // the standard immersive behaviour. Audio keeps the bars, like any other app.
    DisposableEffect(activity) {
        if (!isVideo) return@DisposableEffect onDispose { }
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    if (isVideo) {
        VideoPlayerScreen(state, onBack, activity, viewModel)
    } else {
        AudioPlayerScreen(state, onBack, viewModel)
    }
}

@UnstableApi
@Composable
private fun VideoPlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    activity: Activity?,
    viewModel: PlayerViewModel,
) {
    val equalizerState by viewModel.equalizer.collectAsStateWithLifecycle()

    rememberPlayerActive(autoPip = state.prefs.autoPip)

    var controlsVisible by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.attachSubtitle(it.toString())
            showSubtitleSheet = true
        }
    }

    // Poll the player for position/duration; Media3 has no reactive clock.
    LaunchedEffect(state.controller) {
        while (true) {
            state.controller?.let {
                duration = it.duration.coerceAtLeast(0)
                if (!scrubbing) position = it.currentPosition
            }
            delay(500)
        }
    }

    // Apply the configured default orientation once the video's own size is known, so
    // "Video orientation" has something to follow.
    LaunchedEffect(state.prefs.orientationMode, state.controller) {
        val p = state.controller ?: return@LaunchedEffect
        while (true) {
            val size = p.videoSize
            if (size.width > 0 && size.height > 0) {
                activity?.applyDefaultOrientation(state.prefs.orientationMode, size.width, size.height)
                break
            }
            delay(200)
        }
    }

    // Auto-hide the overlay while playing.
    LaunchedEffect(controlsVisible, state.controller?.isPlaying, state.locked) {
        if (controlsVisible && !state.locked) {
            delay(3_500)
            controlsVisible = false
        }
    }

    // A gesture hint describes the gesture that is happening, so it clears itself
    // shortly after the last update instead of staying on screen for good.
    LaunchedEffect(gestureHint) {
        if (gestureHint != null) {
            delay(900)
            gestureHint = null
        }
    }

    // Keep the PiP window the same shape as the video that is playing, rather than a
    // fixed 16:9 box.
    LaunchedEffect(state.player) {
        val p = state.player ?: return@LaunchedEffect
        while (true) {
            val size = p.videoSize
            if (size.width > 0 && size.height > 0) {
                (activity as? MainActivity)?.updatePictureInPictureAspectRatio(size.width, size.height)
            }
            delay(1_000)
        }
    }

    // Apply the gesture brightness to the window; restore on exit.
    DisposableEffect(state.brightness) {
        val window = activity?.window
        val previous = window?.attributes?.screenBrightness ?: -1f
        window?.let {
            val attrs = it.attributes
            attrs.screenBrightness = state.brightness
            it.attributes = attrs
        }
        onDispose {
            window?.let {
                val attrs = it.attributes
                attrs.screenBrightness = previous
                it.attributes = attrs
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.locked, state.prefs.gesturesEnabled, state.prefs.seekStepSeconds) {
                if (state.locked || !state.prefs.gesturesEnabled) return@pointerInput
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        val step = state.prefs.seekStepSeconds * 1_000L
                        if (offset.x < size.width / 2f) viewModel.seekBy(-step)
                        else viewModel.seekBy(step)
                    },
                )
            }
            .pointerInput(state.locked, state.prefs.gesturesEnabled) {
                if (state.locked || !state.prefs.gesturesEnabled) return@pointerInput
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        if (kotlin.math.abs(dx) > 40f && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            // Read the duration from the controller at the moment of the
                            // swipe: this block is keyed only on the lock and the gesture
                            // setting, so it would otherwise keep the duration that was
                            // current when it was installed.
                            val live = state.controller?.duration ?: 0L
                            viewModel.seekBy(dragSeekDelta(dx / size.width, live))
                        }
                        dx = 0f; dy = 0f
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            val delta = dragSeekDelta(dx / size.width, state.controller?.duration ?: 0L)
                            gestureHint = (if (delta >= 0) "+" else "") + formatClock(delta)
                        } else {
                            val vertical = -dy / size.height
                            if (change.position.x < size.width / 2f) {
                                viewModel.adjustBrightness(vertical)
                                gestureHint = "Brightness ${(viewModel.state.value.brightness * 100).toInt()}%"
                            } else {
                                viewModel.adjustVolume(vertical)
                                gestureHint = "Volume ${(viewModel.state.value.volume * 100).toInt()}%"
                            }
                        }
                    },
                )
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    // Captions are drawn by the Compose overlay below, so the view's
                    // own subtitle view is hidden to avoid showing each line twice.
                    subtitleView?.visibility = View.GONE
                }
            },
            update = { view ->
                view.player = state.player
                view.resizeMode = state.resizeMode.media3Mode
                view.scaleX = if (state.mirrored) -1f else 1f
                view.subtitleView?.visibility = View.GONE
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.nightMode) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
            )
        }

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        gestureHint?.let {
            // Same reasoning as the caption scrim below: a Material Surface over the
            // player's SurfaceView comes out transparent, so paint the background directly.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(
                    it,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        // Captions sit above the controls so they stay on screen when the overlay
        // auto-hides, which is when a subtitle is most useful. The scrim is a plain
        // background on a Box, not a Material Surface: Surface paints its colour inside
        // a render layer, and a translucent layer over the player's SurfaceView does not
        // blend with the video, so the scrim came out transparent and the text floated
        // on the picture. Modifier.background draws into the window's own display list,
        // the same way the night-mode dim below does, and blends correctly.
        state.subtitleText?.let { caption ->
            // White text with a soft outline, the way Arc Player draws captions. No
            // plate behind it, so the picture stays visible; the outline is what keeps
            // the text readable over a light frame.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = if (controlsVisible) 190.dp else 48.dp),
            ) {
                Text(
                    caption,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        drawStyle = Stroke(
                            width = 6f,
                            join = StrokeJoin.Round,
                            cap = StrokeCap.Round,
                        ),
                    ),
                )
            }
        }

        if (state.locked) {
            IconButton(
                onClick = { viewModel.toggleLock(); controlsVisible = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.LockOpen, contentDescription = "Unlock", tint = Color.White)
            }
        } else {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ControlsOverlay(
                    state = state,
                    position = position,
                    duration = duration,
                    scrubbing = scrubbing,
                    scrubPosition = scrubPosition,
                    onBack = onBack,
                    onScrubStart = { scrubbing = true; scrubPosition = position.toFloat() },
                    onScrub = { scrubPosition = it },
                    onScrubEnd = {
                        viewModel.seekTo(scrubPosition.toLong())
                        position = scrubPosition.toLong()
                        scrubbing = false
                    },
                    onTogglePlay = viewModel::togglePlayPause,
                    onSeekBy = viewModel::seekBy,
                    onResume = { viewModel.seekTo(state.savedPositionMillis) },
                    onPrevious = viewModel::skipToPrevious,
                    onNext = viewModel::skipToNext,
                    onSpeed = { showSpeedSheet = true },
                    onSleep = { showSleepSheet = true },
                    onAbRepeat = viewModel::markAbRepeat,
                    onResize = viewModel::cycleResizeMode,
                    onRepeat = viewModel::cycleRepeatMode,
                    onShuffle = viewModel::toggleShuffle,
                    onMirror = viewModel::toggleMirror,
                    onNight = viewModel::toggleNightMode,
                    onLock = { viewModel.toggleLock(); controlsVisible = true },
                    onRotate = { activity?.toggleOrientation() },
                    onPictureInPicture = { (activity as? MainActivity)?.enterPictureInPicture() },
                    onEqualizer = { showEqualizer = true },
                    equalizerEnabled = equalizerState.enabled,
                    onSubtitles = { showSubtitleSheet = true },
                    subtitleActive = state.activeSubtitle != null,
                    subtitleCount = state.subtitleTracks.size,
                    onAudioTracks = { showAudioSheet = true },
                    audioTrackCount = state.audioTracks.size,
                )
            }
        }

        if (showAudioSheet) {
            AudioTrackSheet(
                tracks = state.audioTracks,
                onPick = { viewModel.selectAudioTrack(it); showAudioSheet = false },
                onDismiss = { showAudioSheet = false },
            )
        }

        if (showSubtitleSheet) {
            SubtitleSheet(
                tracks = state.subtitleTracks,
                active = state.activeSubtitle,
                onPick = { viewModel.selectSubtitle(it); showSubtitleSheet = false },
                onTurnOff = { viewModel.selectSubtitle(null); showSubtitleSheet = false },
                onBrowse = { subtitlePicker.launch(arrayOf("*/*")) },
                onDismiss = { showSubtitleSheet = false },
            )
        }

        if (showEqualizer) {
            EqualizerDialog(
                state = equalizerState,
                range = viewModel.equalizerRange(),
                onToggle = viewModel::setEqualizerEnabled,
                onPreset = viewModel::setEqualizerPreset,
                onBand = viewModel::setEqualizerBand,
                onDismiss = { showEqualizer = false },
            )
        }

        if (showSpeedSheet) {
            ChoiceSheet(
                title = "Playback speed",
                options = SPEED_PRESETS.map { formatSpeed(it) to (it == state.speed) },
                onPick = { index -> viewModel.setSpeed(SPEED_PRESETS[index]); showSpeedSheet = false },
                onDismiss = { showSpeedSheet = false },
            )
        }

        if (showSleepSheet) {
            ChoiceSheet(
                title = "Sleep timer",
                options = buildList {
                    SLEEP_TIMER_PRESETS.forEach { add("$it minutes" to false) }
                    add("Off" to (state.sleepRemainingMillis == null))
                },
                onPick = { index ->
                    viewModel.setSleepTimer(if (index < SLEEP_TIMER_PRESETS.size) SLEEP_TIMER_PRESETS[index] else null)
                    showSleepSheet = false
                },
                onDismiss = { showSleepSheet = false },
            )
        }
    }
}

@UnstableApi
@Composable
private fun ControlsOverlay(
    state: PlayerUiState,
    position: Long,
    duration: Long,
    scrubbing: Boolean,
    scrubPosition: Float,
    onBack: () -> Unit,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onAbRepeat: () -> Unit,
    onResize: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onMirror: () -> Unit,
    onNight: () -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
    onPictureInPicture: () -> Unit,
    onEqualizer: () -> Unit,
    equalizerEnabled: Boolean,
    onSubtitles: () -> Unit,
    subtitleActive: Boolean,
    subtitleCount: Int,
    onAudioTracks: () -> Unit,
    audioTrackCount: Int,
) {
    val playing = state.controller?.isPlaying == true
    val shown = if (scrubbing) scrubPosition.toLong() else position
    val remaining = (duration - shown).coerceAtLeast(0L)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = state.title ?: "",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            state.sleepRemainingMillis?.let {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(" " + formatSleepRemaining(it), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(8.dp))
            }
            // Window-level actions live in the top bar. They change how the picture is
            // presented rather than what is playing, and the row at the bottom scrolls
            // horizontally, so keeping them here means rotate and PiP are always one tap
            // away instead of being scrolled off the end.
            IconButton(onClick = onRotate) {
                Icon(Icons.Filled.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
            }
            IconButton(onClick = onPictureInPicture) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture-in-picture", tint = Color.White)
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color.White)
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onSeekBy(-state.prefs.seekStepSeconds * 1_000L) }) {
                Icon(Icons.Filled.Replay10, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (state.hasPrevious) Color.White else Color.White.copy(alpha = 0.38f),
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onNext, enabled = state.hasNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = if (state.hasNext) Color.White else Color.White.copy(alpha = 0.38f),
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = { onSeekBy(state.prefs.seekStepSeconds * 1_000L) }) {
                Icon(Icons.Filled.Forward10, contentDescription = "Fast forward", tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatClock(shown),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
            Slider(
                value = if (scrubbing) scrubPosition else position.toFloat(),
                onValueChange = { if (!scrubbing) onScrubStart(); onScrub(it) },
                onValueChangeFinished = onScrubEnd,
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            // Readouts chosen in Display settings: remaining time, clock, battery.
            val readouts = buildList {
                if (OverlayItem.REMAINING_TIME in state.prefs.overlayItems) {
                    add(if (duration > 0) "−${formatClock(remaining)}" else formatClock(duration))
                }
                if (OverlayItem.CLOCK in state.prefs.overlayItems) {
                    add(clockLabel())
                }
                if (OverlayItem.BATTERY in state.prefs.overlayItems) {
                    add(batteryLabel())
                }
            }
            if (readouts.isNotEmpty()) {
                Text(
                    readouts.joinToString("   "),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OverlayButton(Icons.Filled.Speed, formatSpeed(state.speed), onSpeed)
            // In "ask" mode playback starts at the top, so the saved position is offered
            // as a one-tap jump instead of being applied silently.
            if (state.prefs.resumeMode == ResumeMode.ASK && state.savedPositionMillis > 0) {
                OverlayButton(
                    Icons.Filled.Replay10,
                    "Resume ${formatClock(state.savedPositionMillis)}",
                    onResume,
                )
            }
            OverlayButton(Icons.Filled.AspectRatio, state.resizeMode.label, onResize)
            OverlayButton(
                if (state.repeatMode.media3Mode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                state.repeatMode.label.removePrefix("Repeat ").replaceFirstChar { it.uppercase() },
                onRepeat,
                active = state.repeatMode.media3Mode != 0,
            )
            OverlayButton(Icons.Filled.Shuffle, "Shuffle", onShuffle, active = state.shuffle)
            OverlayButton(Icons.Filled.Timer, "A–B", onAbRepeat, active = state.abRepeat.hasStart)
            OverlayButton(Icons.Filled.Bedtime, "Sleep", onSleep, active = state.sleepRemainingMillis != null)
            OverlayButton(Icons.Filled.Flip, "Mirror", onMirror, active = state.mirrored)
            OverlayButton(Icons.Filled.Brightness6, "Night", onNight, active = state.nightMode)
            OverlayButton(Icons.Filled.Equalizer, "Equalizer", onEqualizer, active = equalizerEnabled)
            OverlayButton(
                Icons.Filled.Subtitles,
                if (subtitleCount > 1) "Subtitle ($subtitleCount)" else "Subtitle",
                onSubtitles,
                active = subtitleActive,
            )
            OverlayButton(
                Icons.Filled.MusicNote,
                if (audioTrackCount > 1) "Audio ($audioTrackCount)" else "Audio",
                onAudioTracks,
                active = audioTrackCount > 1,
            )
        }
    }
}

@Composable
internal fun EqualizerDialog(
    state: com.example.nplayer.playback.EqualizerState,
    range: IntRange,
    onToggle: (Boolean) -> Unit,
    onPreset: (Int) -> Unit,
    onBand: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equalizer") },
        text = {
            if (!state.available) {
                Text("This device has no equalizer, so it cannot be adjusted.")
            } else {
                Column(Modifier.fillMaxHeight(0.75f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", modifier = Modifier.weight(1f))
                        Switch(checked = state.enabled, onCheckedChange = onToggle)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Presets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.presetNames.forEachIndexed { index, name ->
                            TextButton(onClick = { onPreset(index) }) {
                                Text(
                                    name,
                                    color = if (index == state.presetIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                    if (state.isCustom) {
                        Text(
                            "Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        state.bandLevels.forEachIndexed { band, level ->
                            Text(
                                state.bandLabels.getOrNull(band) ?: "Band ${band + 1}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Slider(
                                value = com.example.nplayer.playback.fractionFromLevel(
                                    level,
                                    range.first,
                                    range.last,
                                ),
                                onValueChange = { fraction ->
                                    onBand(
                                        band,
                                        com.example.nplayer.playback.levelFromFraction(
                                            fraction,
                                            range.first,
                                            range.last,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun AudioTrackSheet(
    tracks: List<AudioTrack>,
    onPick: (AudioTrack) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio track") },
        text = {
            Column(Modifier.fillMaxHeight(0.7f).verticalScroll(rememberScrollState())) {
                if (tracks.isEmpty()) {
                    Text(
                        "The tracks are read once the file has opened. If this stays " +
                            "empty, the item has no audio track the device can decode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tracks.forEach { track ->
                        TextButton(onClick = { onPick(track) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (track.selected) "✓ ${track.label}" else track.label,
                                color = if (track.selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                            track.mimeType?.let {
                                Text(
                                    it.substringAfter('/'),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SubtitleSheet(
    tracks: List<SubtitleTrack>,
    active: SubtitleTrack?,
    onPick: (SubtitleTrack) -> Unit,
    onTurnOff: () -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtitles") },
        text = {
            Column(Modifier.fillMaxHeight(0.7f).verticalScroll(rememberScrollState())) {
                if (tracks.isEmpty()) {
                    Text(
                        "No subtitle files were found next to this item, and it has no " +
                            "embedded text track. You can still open a file by hand.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tracks.forEach { track ->
                        TextButton(onClick = { onPick(track) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (track == active) "✓ ${track.label}" else track.label,
                                color = if (track == active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (track.embedded) "Embedded" else "File",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onTurnOff, modifier = Modifier.fillMaxWidth()) {
                    Text("Turn off")
                }
                TextButton(onClick = onBrowse, modifier = Modifier.fillMaxWidth()) {
                    Text("Open a file…")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun OverlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    // The video overlay sits on a dark scrim, so white is the default there. The audio
    // player draws on the normal surface, where white would be invisible.
    inactiveTint: Color = Color.White,
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else inactiveTint
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
internal fun ChoiceSheet(
    title: String,
    options: List<Pair<String, Boolean>>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // The list can be taller than the dialog's 70% cap — the speed sheet has
            // eleven entries and the player is often in landscape, where there is little
            // height to spare — so it has to scroll. Without this the last entries, and
            // with them 4x, were clipped and could not be tapped.
            Column(
                Modifier
                    .fillMaxHeight(0.7f)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEachIndexed { index, (label, selected) ->
                    TextButton(onClick = { onPick(index) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (selected) "✓ $label" else label,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Switches between following the device and a locked landscape view. Rotating is how
 * the viewer asks for fullscreen while watching, so one press locks landscape and the
 * next hands control back to the device; there is no separate portrait lock, which
 * used to leave the app stuck after leaving the player.
 */
private fun Activity.toggleOrientation() {
    requestedOrientation = when (requestedOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

/**
 * The current wall-clock time as "HH:mm", refreshed once a minute while it is shown.
 */
@Composable
private fun clockLabel(): String {
    val time by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            // Sleep to the next minute boundary so the label changes on the minute.
            delay(60_000 - (value % 60_000))
        }
    }
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
}

/**
 * The device battery percentage, or null when the level cannot be read. Registered for
 * the lifetime of the composition so the figure follows the battery while it is shown.
 */
@Composable
private fun batteryLabel(): String? {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val raw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (raw >= 0 && scale > 0) level = raw * 100 / scale
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        level = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return level.takeIf { it >= 0 }?.let { "$it%" }
}

/**
 * Tells [MainActivity] that the player screen is open, so its auto-PiP hook knows
 * whether leaving the app should shrink the video to a floating window.
 */
@Composable
fun rememberPlayerActive(autoPip: Boolean) {
    val context = LocalContext.current
    DisposableEffect(autoPip) {
        val activity = context.findActivity() as? MainActivity
        activity?.setPlayerState(playerActive = true, autoPip = autoPip)
        onDispose { activity?.setPlayerState(playerActive = false, autoPip = autoPip) }
    }
}

/**
 * Hands orientation back to the device. The rotate button locks landscape, and that
 * lock would otherwise outlive the player and hold every later screen sideways.
 */
fun Activity.resetOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

/**
 * Applies the configured default orientation when a video opens. [OrientationMode.VIDEO]
 * waits for the file's own size and is handled by the caller, so it maps to
 * "follow the sensor" until that size is known.
 */
fun Activity.applyDefaultOrientation(mode: OrientationMode, videoWidth: Int, videoHeight: Int) {
    requestedOrientation = when (mode) {
        OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        OrientationMode.VIDEO -> when {
            videoWidth > videoHeight -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            videoHeight > videoWidth -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
