package com.example.nplayer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nplayer.ui.components.ErrorBox
import com.example.nplayer.ui.components.LoadingBox
import com.example.nplayer.ui.components.PosterCard

@Composable
fun HomeScreen(
    onOpenDetail: (String) -> Unit,
    onRequireAuth: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.requiresAuth) {
        if (state.requiresAuth) onRequireAuth()
    }

    when {
        state.isLoading -> LoadingBox()
        state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Recently added",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(state.latest, key = { it.id }) { item ->
                    PosterCard(
                        title = item.name ?: "",
                        imageUrl = viewModel.image(item),
                        subtitle = item.productionYear?.toString(),
                        onClick = { onOpenDetail(item.id) },
                    )
                }
            }
        }
    }
}
