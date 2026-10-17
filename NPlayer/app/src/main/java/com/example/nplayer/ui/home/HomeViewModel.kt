package com.example.nplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.data.prefs.PlaybackSettingsStore
import com.example.nplayer.data.prefs.RecentlyPlayedStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val videos: List<MediaItem> = emptyList(),
    val audio: List<MediaItem> = emptyList(),
    /** Items played most recently, newest first; empty until something has been played. */
    val recentlyPlayed: List<MediaItem> = emptyList(),
    /** False when the Music section is switched off in settings. */
    val showMusic: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && videos.isEmpty() && audio.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val playbackSettings: PlaybackSettingsStore,
    private val recentlyPlayedStore: RecentlyPlayedStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // The Music switch changes what the grid shows, so it is watched rather than
        // read once on entry.
        viewModelScope.launch {
            playbackSettings.settings.collect { prefs ->
                _state.update { it.copy(showMusic = prefs.showMusic) }
            }
        }
        // The history is a flow, so playing something updates the row behind the
        // player screen and it is already current when Home is shown again.
        viewModelScope.launch {
            recentlyPlayedStore.keys.collect { keys ->
                _state.update { it.copy(recentlyPlayed = library.recentlyPlayed(keys)) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            library.refresh()
            _state.update {
                it.copy(
                    isLoading = false,
                    videos = library.videos(),
                    audio = library.audio(),
                    recentlyPlayed = library.recentlyPlayed(recentlyPlayedStore.current()),
                )
            }
        }
    }
}
