package com.example.nplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.remote.model.BaseItemDto
import com.example.nplayer.data.repository.ApiResult
import com.example.nplayer.data.repository.AuthRepository
import com.example.nplayer.data.repository.LibraryRepository
import com.example.nplayer.playback.StreamUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val requiresAuth: Boolean = false,
    val error: String? = null,
    val libraries: List<BaseItemDto> = emptyList(),
    val latest: List<BaseItemDto> = emptyList(),
    val imageUrls: Map<String, String> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val authRepository: AuthRepository,
    private val streamUrlBuilder: StreamUrlBuilder,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val session = authRepository.currentSession()
            if (!session.isAuthenticated) {
                _state.update { it.copy(isLoading = false, requiresAuth = true) }
                return@launch
            }

            val latestResult = libraryRepository.latest(limit = 24)
            when (latestResult) {
                is ApiResult.Success -> {
                    val items = latestResult.data.items
                    val urls = items.associate { item ->
                        item.id to (streamUrlBuilder.imageUrl(item.id, item.primaryImageTag) ?: "")
                    }
                    val libs = (libraryRepository.libraries() as? ApiResult.Success)?.data?.items.orEmpty()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            latest = items,
                            libraries = libs,
                            imageUrls = urls,
                            error = null,
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = "Could not load library")
                }
            }
        }
    }

    fun image(item: BaseItemDto): String? = _state.value.imageUrls[item.id]?.takeIf { it.isNotBlank() }
}
