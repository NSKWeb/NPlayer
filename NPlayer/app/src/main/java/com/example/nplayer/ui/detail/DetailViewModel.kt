package com.example.nplayer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.remote.model.BaseItemDto
import com.example.nplayer.data.repository.ApiResult
import com.example.nplayer.data.repository.LibraryRepository
import com.example.nplayer.playback.StreamUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val item: BaseItemDto? = null,
    val imageUrl: String? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val streamUrlBuilder: StreamUrlBuilder,
) : ViewModel() {

    private val itemId: String = savedStateHandle.get<String>("itemId").orEmpty()

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = libraryRepository.item(itemId)) {
                is ApiResult.Success -> {
                    val item = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            item = item,
                            imageUrl = streamUrlBuilder.imageUrl(item.id, item.primaryImageTag),
                            isFavorite = item.userData?.isFavorite == true,
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = "Could not load item")
                }
            }
        }
    }

    fun toggleFavorite() {
        val newValue = !_state.value.isFavorite
        viewModelScope.launch {
            libraryRepository.markFavorite(itemId, newValue)
            _state.update { it.copy(isFavorite = newValue) }
        }
    }
}
