package com.example.nplayer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.data.prefs.AppSettingsStore
import com.example.nplayer.data.prefs.PlaybackPositionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val item: MediaItem? = null,
    val isFavourite: Boolean = false,
    val resumePositionMillis: Long = 0L,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val library: MediaLibrary,
    private val settingsStore: AppSettingsStore,
    private val positionStore: PlaybackPositionStore,
) : ViewModel() {

    private val itemKey: String = savedStateHandle.get<String>("itemKey").orEmpty()

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            library.refreshIfEmpty()
            val item = library.find(itemKey)
            val settings = settingsStore.current()
            val position = positionStore.position(itemKey)
            _state.update {
                it.copy(
                    item = item,
                    isFavourite = itemKey in settings.favourites,
                    resumePositionMillis = position,
                )
            }
        }
    }

    fun toggleFavourite() {
        viewModelScope.launch {
            val favourites = settingsStore.toggleFavourite(itemKey)
            _state.update { it.copy(isFavourite = itemKey in favourites) }
        }
    }
}
