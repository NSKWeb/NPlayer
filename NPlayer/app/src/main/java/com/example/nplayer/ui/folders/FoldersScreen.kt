package com.example.nplayer.ui.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nplayer.data.media.FolderCrumb
import com.example.nplayer.data.media.FolderSummary
import com.example.nplayer.data.media.MediaKind
import com.example.nplayer.data.media.breadcrumbsFor
import com.example.nplayer.data.media.isRootFolder
import com.example.nplayer.data.media.parentFolder
import com.example.nplayer.ui.components.LoadingBox
import com.example.nplayer.ui.components.MessageState
import com.example.nplayer.ui.components.PosterCard

@Composable
fun FoldersScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        !state.isLoaded -> LoadingBox()

        isRootFolder(state.folder) && state.folders.isEmpty() && state.items.isEmpty() ->
            MessageState(
                title = "No folders yet",
                body = "Nothing was found on this device. Copy some files over, then " +
                    "scan again.",
                actionLabel = "Scan again",
                onAction = viewModel::refresh,
            )

        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Breadcrumbs(
                    crumbs = breadcrumbsFor(state.folder),
                    onOpen = viewModel::open,
                    onUp = { parentFolder(state.folder)?.let(viewModel::open) },
                )
            }

            if (state.folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Folders · ${state.folders.size}")
                }
                items(state.folders, key = { it.path }) { folder ->
                    FolderRow(folder = folder, onClick = { viewModel.open(folder.path) })
                }
            }

            if (state.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Files · ${state.items.size}")
                }
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
}

/** Path trail; every crumb except the last is tappable, and "Up" walks one level. */
@Composable
private fun Breadcrumbs(
    crumbs: List<FolderCrumb>,
    onOpen: (String) -> Unit,
    onUp: () -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (crumbs.size > 1) {
            Text(
                text = "Up",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onUp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .clickable(enabled = !isLast) { onOpen(crumb.path) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FolderRow(folder: FolderSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = folder.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            text = "${folder.itemCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}
