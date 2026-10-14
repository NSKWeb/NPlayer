package com.example.nplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.data.playlists.Playlist
import com.example.nplayer.data.playlists.PlaylistStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val store: PlaylistStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistsUiState())
    val state: StateFlow<PlaylistsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.playlists.collect { playlists ->
                _state.update { PlaylistsUiState(playlists = playlists, isLoaded = true) }
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch { store.create(name) }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }
}

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val items: List<MediaItem> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val store: PlaylistStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private var playlistId: String = ""

    fun onEnter(id: String) {
        playlistId = id
        viewModelScope.launch {
            library.refreshIfEmpty()
            store.playlists.collect { playlists ->
                val playlist = playlists.firstOrNull { it.id == playlistId }
                _state.update {
                    PlaylistDetailUiState(
                        playlist = playlist,
                        // Items that have been deleted since simply drop out.
                        items = playlist?.itemKeys?.mapNotNull { key -> library.findVisible(key) }.orEmpty(),
                        isLoaded = true,
                    )
                }
            }
        }
    }

    fun remove(itemKey: String) {
        viewModelScope.launch { store.removeItem(playlistId, itemKey) }
    }

    fun rename(name: String) {
        viewModelScope.launch { store.rename(playlistId, name) }
    }
}
