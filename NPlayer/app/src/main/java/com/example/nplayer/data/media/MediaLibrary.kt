package com.example.nplayer.data.media

import com.example.nplayer.data.lyrics.pickLyricsFile
import com.example.nplayer.data.prefs.AppSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.launch
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
    private val settingsStore: AppSettingsStore,
) {
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanMutex = Mutex()

    /**
     * Keys in the private folder. Every listing filters these out, so a hidden item
     * never shows up in Home, Search, Folders or Favourites; only the gated private
     * screen reads [allItems].
     */
    private val hiddenKeys = MutableStateFlow<Set<String>>(emptySet())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            settingsStore.settings.collect { hiddenKeys.value = it.privateItems }
        }
    }

    /** Every scanned item, including private ones. Only for the private screen. */
    val allItems: StateFlow<List<MediaItem>> get() = _items.asStateFlow()

    /** Re-reads MediaStore. Concurrent calls share one scan. */
    suspend fun refresh() = scanMutex.withLock {
        _isScanning.value = true
        try {
            _items.value = repository.scan()
            _subtitleFiles.value = repository.scanSubtitles()
            _lyricsFiles.value = repository.scanLyrics()
        } finally {
            _isScanning.value = false
        }
    }

    /** Subtitle files on the device, keyed by lower-case file name. */
    private val _subtitleFiles = MutableStateFlow<Map<String, SubtitleFile>>(emptyMap())

    /** Lyrics files on the device, keyed by lower-case file name. */
    private val _lyricsFiles = MutableStateFlow<Map<String, LyricsFile>>(emptyMap())

    /**
     * The lyrics file for [item], or null. Unlike subtitles a track has at most one
     * lyric file shown, so the best name match is picked rather than the whole set.
     */
    fun lyricsFor(item: MediaItem): LyricsFile? {
        val folder = normalizeFolder(item.folder)
        val candidates = _lyricsFiles.value.values
            .filter { normalizeFolder(it.folder) == folder }
            .map { it.name }
        val name = pickLyricsFile(item.displayName, candidates) ?: return null
        return _lyricsFiles.value[name.lowercase()]
    }

    /**
     * Sidecar subtitle files sitting in [item]'s folder whose name matches the media
     * file. Returns empty when the item is unknown or nothing matches.
     */
    fun sidecarsFor(item: MediaItem): List<SubtitleFile> {
        val folder = normalizeFolder(item.folder)
        return _subtitleFiles.value.values
            .filter { normalizeFolder(it.folder) == folder && sidecarMatchesMedia(item.displayName, it.name) }
            .sortedBy { it.name.lowercase() }
    }

    /** Scans only if nothing has been loaded yet, so first launch populates itself. */
    suspend fun refreshIfEmpty() {
        if (_items.value.isEmpty()) refresh()
    }

    /** Any scanned item, private or not. Use [findVisible] for ordinary screens. */
    fun find(key: String): MediaItem? = _items.value.firstOrNull { it.key == key }

    /**
     * Like [find], but returns null for an item in the private folder, so a screen
     * cannot surface a hidden item through a stale favourite or playlist entry.
     */
    fun findVisible(key: String): MediaItem? =
        find(key)?.takeIf { it.key !in hiddenKeys.value }

    /** Visible items only; anything in the private folder is withheld. */
    private var cacheItems: List<MediaItem>? = null
    private var cacheHidden: Set<String>? = null
    private var cacheVisible: List<MediaItem> = emptyList()

    /**
     * A screen load calls this more than once (Home asks for videos and audio,
     * Folders for sub-folders and items), so the filtered list is reused until the
     * scan or the hidden set actually changes.
     */
    @Synchronized
    private fun visible(): List<MediaItem> {
        val items = _items.value
        val hidden = hiddenKeys.value
        if (items !== cacheItems || hidden !== cacheHidden) {
            cacheItems = items
            cacheHidden = hidden
            cacheVisible = items.filterNot { it.key in hidden }
        }
        return cacheVisible
    }

    fun videos(): List<MediaItem> = visible().filter { it.kind == MediaKind.VIDEO }

    fun audio(): List<MediaItem> = visible().filter { it.kind == MediaKind.AUDIO }

    /** The immediate child folders of [folder] that contain something, with counts. */
    fun foldersIn(folder: String): List<FolderSummary> =
        childFolders(folder, visible().map { it.folder })

    /** Items directly inside [folder], newest first, excluding items in sub-folders. */
    fun itemsIn(folder: String): List<MediaItem> {
        val normalized = normalizeFolder(folder)
        return visible()
            .filter { normalizeFolder(it.folder) == normalized }
            .sortedByDescending { it.dateAddedSeconds }
    }

    fun search(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        return visible().filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                item.album?.contains(query, ignoreCase = true) == true ||
                item.artist?.contains(query, ignoreCase = true) == true
        }
    }
}
