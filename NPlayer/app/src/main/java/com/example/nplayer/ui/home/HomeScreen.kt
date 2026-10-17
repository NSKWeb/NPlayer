package com.example.nplayer.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nplayer.data.media.MediaKind
import com.example.nplayer.ui.components.LoadingBox
import com.example.nplayer.ui.components.MediaGridCells
import com.example.nplayer.ui.components.MediaGridHorizontalSpacing
import com.example.nplayer.ui.components.MediaGridPadding
import com.example.nplayer.ui.components.MediaGridVerticalSpacing
import com.example.nplayer.ui.components.MessageState
import com.example.nplayer.ui.components.PosterCard
import com.example.nplayer.ui.components.SectionHeader

/** The media permissions this app needs, resolved for the running API level. */
private fun requiredMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun HomeScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun hasPermission() = requiredMediaPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(hasPermission()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result.values.all { it } || hasPermission()
    }

    LaunchedEffect(granted) {
        if (granted) viewModel.refresh()
    }

    when {
        !granted -> MessageState(
            title = "N Player needs access to your media",
            body = "Your videos and music stay on this device. Nothing is uploaded, and " +
                "there is no account or server to set up.",
            actionLabel = "Allow access",
            onAction = { launcher.launch(requiredMediaPermissions()) },
        )

        state.isLoading -> LoadingBox()

        state.isEmpty -> MessageState(
            title = "Nothing to play yet",
            body = "No video or audio was found on this device. Copy some files over, " +
                "then pull to refresh.",
            actionLabel = "Scan again",
            onAction = viewModel::refresh,
        )

        else -> LazyVerticalGrid(
            columns = MediaGridCells,
            contentPadding = MediaGridPadding,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                MediaGridHorizontalSpacing,
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                MediaGridVerticalSpacing,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.recentlyPlayed.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Recently played", count = state.recentlyPlayed.size)
                }
                items(state.recentlyPlayed, key = { "recent-${it.key}" }) { item ->
                    // The history mixes video and audio, so each card follows its own
                    // kind the way the Videos and Music sections do. The key is prefixed
                    // because the same item also appears in its own section below, and
                    // LazyGrid requires keys to be unique across the grid.
                    val isVideo = item.kind == MediaKind.VIDEO
                    PosterCard(
                        title = item.title,
                        imageModel = if (isVideo) item.uri else null,
                        subtitle = item.folder.ifBlank { item.resolution ?: item.sizeLabel },
                        aspectRatio = if (isVideo) 16f / 9f else 2f / 3f,
                        placeholderIcon = if (isVideo) Icons.Filled.Movie else Icons.Filled.MusicNote,
                        badge = item.durationLabel,
                        onClick = { onOpenDetail(item.key) },
                    )
                }
            }

            if (state.videos.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Videos", count = state.videos.size)
                }
                items(state.videos, key = { it.key }) { item ->
                    PosterCard(
                        title = item.title,
                        imageModel = item.uri,
                        subtitle = item.resolution ?: item.sizeLabel,
                        aspectRatio = 16f / 9f,
                        placeholderIcon = Icons.Filled.Movie,
                        badge = item.durationLabel,
                        onClick = { onOpenDetail(item.key) },
                    )
                }
            }

            if (state.showMusic && state.audio.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Music", count = state.audio.size)
                }
                items(state.audio, key = { it.key }) { item ->
                    PosterCard(
                        title = item.title,
                        imageModel = null,
                        subtitle = item.artist ?: item.album,
                        badge = item.durationLabel,
                        onClick = { onOpenDetail(item.key) },
                    )
                }
            }
        }
    }
}
