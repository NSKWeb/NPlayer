package com.example.nplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
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
) {
    val isEmpty: Boolean get() = !isLoading && videos.isEmpty() && audio.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val library: MediaLibrary,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            library.refresh()
            _state.update {
                it.copy(
                    isLoading = false,
                    videos = library.videos(),
                    audio = library.audio(),
                )
            }
        }
    }
}
