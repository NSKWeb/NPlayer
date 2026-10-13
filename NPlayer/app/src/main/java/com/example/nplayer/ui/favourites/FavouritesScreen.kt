package com.example.nplayer.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nplayer.data.media.MediaKind
import com.example.nplayer.ui.components.LoadingBox
import com.example.nplayer.ui.components.MessageState
import com.example.nplayer.ui.components.PosterCard

@Composable
fun FavouritesScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        !state.isLoaded -> LoadingBox()

        state.items.isEmpty() -> MessageState(
            title = "No favourites yet",
            body = "Open any video or track and tap the heart to keep it here.",
        )

        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.items, key = { it.key }) { item ->
                val isVideo = item.kind == MediaKind.VIDEO
                PosterCard(
                    title = item.title,
                    imageModel = if (isVideo) item.uri else null,
                    subtitle = item.artist ?: item.album ?: item.durationLabel,
                    aspectRatio = if (isVideo) 16f / 9f else 2f / 3f,
                    placeholderIcon = Icons.Filled.Movie,
                    onClick = { onOpenDetail(item.key) },
                )
            }
        }
    }
}
