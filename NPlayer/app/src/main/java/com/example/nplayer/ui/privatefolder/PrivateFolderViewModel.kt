package com.example.nplayer.ui.privatefolder

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

data class PrivateFolderUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class PrivateFolderViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivateFolderUiState())
    val state: StateFlow<PrivateFolderUiState> = _state.asStateFlow()

    /** Only called after the biometric gate has unlocked the screen. */
    fun load() {
        viewModelScope.launch {
            library.refreshIfEmpty()
            publish()
        }
    }

    fun removeFromPrivate(itemKey: String) {
        viewModelScope.launch {
            settingsStore.togglePrivate(itemKey)
            publish()
        }
    }

    private suspend fun publish() {
        val privateKeys = settingsStore.current().privateItems
        // Reads the unfiltered list, which is why this screen is gated.
        val items = library.allItems.value.filter { it.key in privateKeys }
        _state.update { PrivateFolderUiState(items = items, isLoaded = true) }
    }
}
