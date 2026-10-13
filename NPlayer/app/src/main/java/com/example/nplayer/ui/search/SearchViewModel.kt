package com.example.nplayer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.remote.model.BaseItemDto
import com.example.nplayer.data.repository.ApiResult
import com.example.nplayer.data.repository.LibraryRepository
import com.example.nplayer.playback.StreamUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<BaseItemDto> = emptyList(),
    val imageUrls: Map<String, String> = emptyMap(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val streamUrlBuilder: StreamUrlBuilder,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        if (value.length < 2) {
            _state.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _state.update { it.copy(isLoading = true) }
            when (val result = libraryRepository.search(value)) {
                is ApiResult.Success -> {
                    val items = result.data.items
                    val urls = items.associate { item ->
                        item.id to (streamUrlBuilder.imageUrl(item.id, item.primaryImageTag) ?: "")
                    }
                    _state.update { it.copy(isLoading = false, results = items, imageUrls = urls) }
                }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, results = emptyList()) }
            }
        }
    }

    fun image(item: BaseItemDto): String? = _state.value.imageUrls[item.id]?.takeIf { it.isNotBlank() }
}
