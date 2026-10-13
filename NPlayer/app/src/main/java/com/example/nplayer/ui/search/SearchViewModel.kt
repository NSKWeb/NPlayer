package com.example.nplayer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
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
    val results: List<MediaItem> = emptyList(),
) {
    val hasQuery: Boolean get() = query.length >= 2
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val library: MediaLibrary,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()

        if (value.length < 2) {
            _state.update { it.copy(results = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(200) // debounce keystrokes
            library.refreshIfEmpty()
            _state.update { it.copy(results = library.search(value)) }
        }
    }
}
