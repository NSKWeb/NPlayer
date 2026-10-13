package com.example.nplayer.data.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the scanned library in memory and scans it on demand. A MediaStore query is
 * cheap enough that caching to disk would add staleness for little gain.
 */
@Singleton
class MediaLibrary @Inject constructor(
    private val repository: MediaStoreRepository,
) {
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanMutex = Mutex()

    /** Re-reads MediaStore. Concurrent calls share one scan. */
    suspend fun refresh() = scanMutex.withLock {
        _isScanning.value = true
        try {
            _items.value = repository.scan()
        } finally {
            _isScanning.value = false
        }
    }

    /** Scans only if nothing has been loaded yet, so first launch populates itself. */
    suspend fun refreshIfEmpty() {
        if (_items.value.isEmpty()) refresh()
    }

    fun find(key: String): MediaItem? = _items.value.firstOrNull { it.key == key }

    fun videos(): List<MediaItem> = _items.value.filter { it.kind == MediaKind.VIDEO }

    fun audio(): List<MediaItem> = _items.value.filter { it.kind == MediaKind.AUDIO }

    fun search(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        return _items.value.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                item.album?.contains(query, ignoreCase = true) == true ||
                item.artist?.contains(query, ignoreCase = true) == true
        }
    }
}
