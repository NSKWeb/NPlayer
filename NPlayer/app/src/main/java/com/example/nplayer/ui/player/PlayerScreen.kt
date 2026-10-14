package com.example.nplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.nplayer.MainActivity
import com.example.nplayer.playback.SLEEP_TIMER_PRESETS
import com.example.nplayer.playback.SPEED_PRESETS
import com.example.nplayer.playback.formatClock
import com.example.nplayer.playback.formatSleepRemaining
import com.example.nplayer.playback.formatSpeed
import kotlinx.coroutines.delay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextAlign
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
    val equalizerState by viewModel.equalizer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

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

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.attachSubtitle(it.toString())
            showSubtitleSheet = true
        }
    }

    DisposableEffect(Unit) {
        viewModel.onEnter(itemKey, queuePlaylistId)
        onDispose { viewModel.onLeave(itemKey) }
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

    // Auto-hide the overlay while playing.
    LaunchedEffect(controlsVisible, state.controller?.isPlaying, state.locked) {
        if (controlsVisible && !state.locked) {
            delay(3_500)
            controlsVisible = false
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
            .pointerInput(state.locked) {
                if (state.locked) return@pointerInput
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2f) viewModel.seekBy(-10_000)
                        else viewModel.seekBy(10_000)
                    },
                )
            }
            .pointerInput(state.locked) {
                if (state.locked) return@pointerInput
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        if (kotlin.math.abs(dx) > 40f && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            viewModel.seekBy((dx / size.width * duration).toLong())
                        }
                        dx = 0f; dy = 0f
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            val delta = (dx / size.width * duration).toLong()
                            gestureHint = (if (delta >= 0) "+" else "") + formatClock(delta)
                        } else {
                            val vertical = -dy / size.height
                            if (change.position.x < size.width / 2f) {
                                viewModel.adjustBrightness(vertical)
                                gestureHint = "Brightness"
                            } else {
                                viewModel.adjustVolume(vertical)
                                gestureHint = "Volume"
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
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    it,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }

        // Captions sit above the controls so they stay on screen when the overlay
        // auto-hides, which is when a subtitle is most useful.
        state.subtitleText?.let { caption ->
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = if (controlsVisible) 190.dp else 48.dp),
            ) {
                Text(
                    caption,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                )
            }
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
            IconButton(onClick = { onSeekBy(-10_000) }) {
                Icon(Icons.Filled.Replay10, contentDescription = "Back 10 seconds", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.width(20.dp))
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
            Spacer(Modifier.width(20.dp))
            IconButton(onClick = { onSeekBy(10_000) }) {
                Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(38.dp))
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
            // Time left in the file, so the length is readable at a glance.
            Text(
                if (duration > 0) "−${formatClock(remaining)}" else formatClock(duration),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OverlayButton(Icons.Filled.Speed, formatSpeed(state.speed), onSpeed)
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
            OverlayButton(Icons.Filled.ScreenRotation, "Rotate", onRotate)
            OverlayButton(Icons.Filled.PictureInPictureAlt, "PiP", onPictureInPicture)
            OverlayButton(Icons.Filled.Equalizer, "Equalizer", onEqualizer, active = equalizerEnabled)
            OverlayButton(
                Icons.Filled.Subtitles,
                if (subtitleCount > 1) "Subtitle ($subtitleCount)" else "Subtitle",
                onSubtitles,
                active = subtitleActive,
            )
        }
    }
}

@Composable
private fun EqualizerDialog(
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
private fun OverlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else Color.White
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ChoiceSheet(
    title: String,
    options: List<Pair<String, Boolean>>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxHeight(0.7f)) {
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

/** Cycles the window between auto, landscape, and portrait. */
private fun Activity.toggleOrientation() {
    requestedOrientation = when (requestedOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
