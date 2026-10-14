package com.example.nplayer.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.data.prefs.AppSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavouritesUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(FavouritesUiState())
    val state: StateFlow<FavouritesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            library.refreshIfEmpty()
            val favourites = settingsStore.current().favourites
            // Favourites are stored as keys, so drop any whose file has since gone,
            // and any the user has since moved into the private folder.
            val items = favourites.mapNotNull { library.findVisible(it) }
            _state.update { FavouritesUiState(items = items, isLoaded = true) }
        }
    }
}
