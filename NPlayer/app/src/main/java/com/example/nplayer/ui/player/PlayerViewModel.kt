package com.example.nplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.nplayer.data.repository.ApiResult
import com.example.nplayer.data.repository.LibraryRepository
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
    private val libraryRepository: LibraryRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun onEnter(itemId: String) {
        viewModelScope.launch {
            val future = playbackController.connect()
            val controller = future.get()
            _state.update { it.copy(player = controller, controller = controller) }

            when (val result = libraryRepository.item(itemId)) {
                is ApiResult.Success -> {
                    val item = result.data
                    _state.update { it.copy(title = item.name) }
                    playbackController.play(item)
                }
                is ApiResult.Failure -> _state.update { it.copy(error = "Could not start playback") }
            }
        }
    }

    fun onLeave(itemId: String) {
        viewModelScope.launch { playbackController.stop(itemId) }
        playbackController.release()
    }
}
