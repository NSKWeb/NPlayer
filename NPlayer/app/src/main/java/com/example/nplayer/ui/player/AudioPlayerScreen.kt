package com.example.nplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.example.nplayer.data.lyrics.Lyrics
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.playback.SPEED_PRESETS
import com.example.nplayer.playback.SLEEP_TIMER_PRESETS
import com.example.nplayer.playback.formatClock
import com.example.nplayer.playback.formatSleepRemaining
import com.example.nplayer.playback.formatSpeed
import kotlinx.coroutines.delay

/**
 * The audio player. Audio has no picture, so this is a music-player layout rather than
 * the video overlay: large artwork area, title/artist/album, a seek bar and transport,
 * and the controls that only make sense for sound — speed, sleep timer, equalizer,
 * shuffle and repeat. Video-only controls (resize, mirror, night mode, picture-in-
 * picture, rotate, subtitles) are deliberately absent here.
 */
@UnstableApi
@Composable
internal fun AudioPlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    viewModel: PlayerViewModel,
) {
    val equalizerState by viewModel.equalizer.collectAsStateWithLifecycle()

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }

    // Poll the player for position/duration; Media3 has no reactive clock.
    LaunchedEffect(state.controller) {
        while (true) {
            state.controller?.let {
                duration = it.duration.coerceAtLeast(0)
                if (!scrubbing) position = it.currentPosition
            }
            delay(250)
        }
    }

    val item = state.currentItem
    val playing = state.controller?.isPlaying == true
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            state.sleepRemainingMillis?.let {
                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + formatSleepRemaining(it), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Artwork placeholder: audio files carry no embedded art here, so a large note
        // on a tonal square stands in for the album cover.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(96.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = item?.title ?: state.title.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = listOfNotNull(item?.artist, item?.album).firstOrNull { !it.isNullOrBlank() }
        Text(
            text = subtitle ?: item?.folder.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(16.dp))

        Slider(
            value = if (scrubbing) scrubPosition else position.toFloat(),
            onValueChange = { if (!scrubbing) scrubbing = true; scrubPosition = it },
            onValueChangeFinished = {
                viewModel.seekTo(scrubPosition.toLong())
                scrubbing = false
            },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatClock(if (scrubbing) scrubPosition.toLong() else position), style = MaterialTheme.typography.labelMedium)
            Text(if (duration > 0) formatClock(duration) else "--:--", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffle) MaterialTheme.colorScheme.primary else onSurfaceVariant,
                )
            }
            FilledTonalIconButton(
                onClick = viewModel::skipToPrevious,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(30.dp))
            }
            FilledIconButton(
                onClick = viewModel::togglePlayPause,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledTonalIconButton(
                onClick = viewModel::skipToNext,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = viewModel::cycleRepeatMode) {
                Icon(
                    if (state.repeatMode.media3Mode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = state.repeatMode.label,
                    tint = if (state.repeatMode.media3Mode != 0) MaterialTheme.colorScheme.primary
                    else onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OverlayButton(Icons.Filled.Speed, formatSpeed(state.speed), { showSpeedSheet = true }, inactiveTint = onSurfaceVariant)
            OverlayButton(Icons.Filled.Equalizer, "Equalizer", { showEqualizer = true }, active = equalizerState.enabled, inactiveTint = onSurfaceVariant)
            OverlayButton(Icons.Filled.Bedtime, "Sleep", { showSleepSheet = true }, active = state.sleepRemainingMillis != null, inactiveTint = onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))

        LyricsPane(lyrics = state.lyrics, activeIndex = state.activeLyric)

        UpNextList(queue = state.queue, current = item)

        Spacer(Modifier.height(12.dp))
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

/**
 * The timed lyrics for the current track. The line playing now is highlighted and the
 * pane scrolls to keep it in view; lines already sung are dimmed. Rendered as a plain
 * column because it lives inside the screen's scrollable column, and it disappears
 * entirely when the track has no lyrics sidecar.
 */
@Composable
private fun LyricsPane(lyrics: Lyrics, activeIndex: Int) {
    if (lyrics.isEmpty) return

    val scrollState = rememberScrollState()
    // Keep the active line centred-ish by scrolling it into view as it advances.
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val fraction = activeIndex.toFloat() / lyrics.lines.size
            scrollState.animateScrollTo((scrollState.maxValue * fraction).toInt())
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Lyrics",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(scrollState),
        ) {
            lyrics.lines.forEachIndexed { index, line ->
                val isActive = index == activeIndex
                Text(
                    text = line.text.ifBlank { "♪" },
                    style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        index < activeIndex -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
    }
}

/**
 * What plays after the current track, in queue order, with the track playing now marked
 * so the list doubles as a position indicator. It is a plain column, not a lazy list,
 * because it lives inside the screen's scrollable column.
 */
@Composable
private fun UpNextList(queue: List<MediaItem>, current: MediaItem?) {
    if (queue.size <= 1) return
    val currentIndex = queue.indexOfFirst { it.key == current?.key }

    Text(
        "Up next",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        queue.forEachIndexed { index, track ->
            val isCurrent = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (isCurrent) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                track.durationLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
