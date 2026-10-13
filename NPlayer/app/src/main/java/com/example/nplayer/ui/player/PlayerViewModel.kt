package com.example.nplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.nplayer.data.media.MediaLibrary
import com.example.nplayer.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
data class PlayerUiState(
    val player: androidx.media3.common.Player? = null,
    val controller: MediaController? = null,
    val title: String? = null,
    val error: String? = null,
)

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun onEnter(itemKey: String) {
        viewModelScope.launch {
            val controller = playbackController.connect().get()
            _state.update { it.copy(player = controller, controller = controller) }

            library.refreshIfEmpty()
            val item = library.find(itemKey)
            if (item == null) {
                _state.update { it.copy(error = "That file is no longer available") }
                return@launch
            }
            _state.update { it.copy(title = item.title) }
            playbackController.play(item)
        }
    }

    fun onLeave(itemKey: String) {
        viewModelScope.launch { playbackController.stop(itemKey) }
        playbackController.release()
    }
}
