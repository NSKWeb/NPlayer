package com.example.nplayer.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.media.FolderSummary
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoldersUiState(
    val folder: String = "",
    val folders: List<FolderSummary> = emptyList(),
    val items: List<MediaItem> = emptyList(),
    val isLoaded: Boolean = false,
    /** Whether folders are listed above files, as the arrangement setting asks. */
    val foldersFirst: Boolean = true,
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val library: MediaLibrary,
) : ViewModel() {

    private val _state = MutableStateFlow(FoldersUiState())
    val state: StateFlow<FoldersUiState> = _state.asStateFlow()

    init {
        // Arrangement can change while this screen is open, so it is watched.
        viewModelScope.launch {
            library.arrangement.collect {
                _state.update { s -> s.copy(foldersFirst = library.foldersFirst()) }
            }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            library.refreshIfEmpty()
            publish()
        }
    }

    /** Opens [folder], which may be a path or the empty root. */
    fun open(folder: String) {
        _state.update { it.copy(folder = folder) }
        publish()
    }

    fun refresh() {
        viewModelScope.launch {
            library.refresh()
            publish()
        }
    }

    private fun publish() {
        val folder = _state.value.folder
        _state.update {
            it.copy(
                folders = library.foldersIn(folder),
                items = library.itemsIn(folder),
                foldersFirst = library.foldersFirst(),
                isLoaded = true,
            )
        }
    }
}
