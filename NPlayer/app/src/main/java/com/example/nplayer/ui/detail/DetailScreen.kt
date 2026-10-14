package com.example.nplayer.ui.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.nplayer.data.media.MediaKind
import com.example.nplayer.data.playlists.Playlist
import com.example.nplayer.ui.components.MessageState
import com.example.nplayer.ui.components.thumbnailRequest

@Composable
fun DetailScreen(
    itemKey: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val context = LocalContext.current
    var showPlaylistPicker by remember { mutableStateOf(false) }

    if (item == null) {
        MessageState(
            title = "File not found",
            body = "This item is no longer on the device. It may have been moved or deleted.",
            actionLabel = "Back",
            onAction = onBack,
        )
        return
    }

    val isVideo = item.kind == MediaKind.VIDEO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isVideo) 16f / 9f else 1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isVideo) {
                AsyncImage(
                    model = thumbnailRequest(context, item.uri),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Fades the artwork into the page so the header does not end abruptly.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background,
                            ),
                        ),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp),
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }

            if (state.isPrivate) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    Icon(
                        Icons.Filled.VisibilityOff,
                        contentDescription = "Hidden",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(18.dp),
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            val meta = listOfNotNull(
                item.artist,
                item.album,
                item.durationLabel,
                item.resolution,
                item.sizeLabel,
            ).joinToString("  •  ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.resumePositionMillis > 0) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "Resumes at ${formatTimestamp(state.resumePositionMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onPlay(item.key) },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(if (state.resumePositionMillis > 0) "  Resume" else "  Play")
                }
                FilledTonalIconButton(
                    onClick = viewModel::toggleFavourite,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (state.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favourite",
                    )
                }
                FilledTonalIconButton(
                    onClick = { showPlaylistPicker = true },
                    shape = CircleShape,
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                }
                FilledTonalIconButton(
                    onClick = viewModel::togglePrivate,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (state.isPrivate) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (state.isPrivate) "Unhide" else "Hide",
                    )
                }
            }

            if (state.isPrivate) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hidden in the private folder",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showPlaylistPicker) {
        AddToPlaylistDialog(
            playlists = state.playlists,
            onPick = { viewModel.addToPlaylist(it); showPlaylistPicker = false },
            onCreate = { viewModel.createPlaylistWithItem(it); showPlaylistPicker = false },
            onDismiss = { showPlaylistPicker = false },
        )
    }

    state.addedToPlaylist?.let { name ->
        AlertDialog(
            onDismissRequest = viewModel::clearAddedMessage,
            title = { Text("Added to playlist") },
            text = { Text("\"${item.title}\" was added to $name.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearAddedMessage) { Text("OK") }
            },
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet. Type a name to make one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onPick(playlist.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(playlist.name, modifier = Modifier.weight(1f))
                            Text("${playlist.size}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("New playlist") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(newName) }, enabled = newName.isNotBlank()) {
                Text("Create and add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatTimestamp(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
