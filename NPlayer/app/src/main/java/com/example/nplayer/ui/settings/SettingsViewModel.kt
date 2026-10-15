package com.example.nplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.CacheManager
import com.example.nplayer.data.formatBytes
import com.example.nplayer.data.media.FileSortOrder
import com.example.nplayer.data.media.FolderSortOrder
import com.example.nplayer.data.prefs.AppSettingsStore
import com.example.nplayer.data.prefs.OrientationMode
import com.example.nplayer.data.prefs.OverlayItem
import com.example.nplayer.data.prefs.PlaybackSettingsStore
import com.example.nplayer.data.prefs.ResumeMode
import com.example.nplayer.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val biometricLock: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val favouriteCount: Int = 0,
    // Playback
    val resumeMode: ResumeMode = ResumeMode.ASK,
    val rememberBrightness: Boolean = false,
    val rememberBackgroundPlay: Boolean = false,
    val rememberAspectRatio: Boolean = false,
    val rememberSpeed: Boolean = false,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
    val seekStepSeconds: Int = 10,
    val autoplayNext: Boolean = true,
    val autoPip: Boolean = false,
    val gesturesEnabled: Boolean = true,
    // Subtitles
    val showSubtitlesByDefault: Boolean = true,
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
    // Display / music
    val overlayItems: Set<OverlayItem> = setOf(OverlayItem.REMAINING_TIME),
    val showMusic: Boolean = true,
    // File arrangement
    val fileSortOrder: FileSortOrder = FileSortOrder.DATE_NEWEST,
    val folderSortOrder: FolderSortOrder = FolderSortOrder.NAME_ASC,
    val foldersFirst: Boolean = true,
    /** Cache size on disk, formatted for display. */
    val cacheSize: String = "0 B",
    /** Set after a clear so the screen can confirm how much was freed. */
    val lastCleared: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val playbackSettings: PlaybackSettingsStore,
    private val cacheManager: CacheManager,
) : ViewModel() {

    private val _cacheSize = MutableStateFlow(cacheManager.cacheSizeBytes())
    private val _lastCleared = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        settingsStore.settings,
        playbackSettings.settings,
        _cacheSize,
        _lastCleared,
    ) { app, playback, cacheBytes, cleared ->
        SettingsUiState(
            biometricLock = app.biometricLockEnabled,
            themeMode = app.themeMode,
            dynamicColor = app.dynamicColor,
            favouriteCount = app.favourites.size,
            resumeMode = playback.resumeMode,
            rememberBrightness = playback.rememberBrightness,
            rememberBackgroundPlay = playback.rememberBackgroundPlay,
            rememberAspectRatio = playback.rememberAspectRatio,
            rememberSpeed = playback.rememberSpeed,
            orientationMode = playback.orientationMode,
            seekStepSeconds = playback.seekStepSeconds,
            autoplayNext = playback.autoplayNext,
            autoPip = playback.autoPip,
            gesturesEnabled = playback.gesturesEnabled,
            showSubtitlesByDefault = playback.showSubtitlesByDefault,
            preferredAudioLanguage = playback.preferredAudioLanguage,
            preferredSubtitleLanguage = playback.preferredSubtitleLanguage,
            overlayItems = playback.overlayItems,
            showMusic = playback.showMusic,
            fileSortOrder = playback.fileSortOrder,
            folderSortOrder = playback.folderSortOrder,
            foldersFirst = playback.foldersFirst,
            cacheSize = formatBytes(cacheBytes),
            lastCleared = cleared,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setBiometricLock(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    // --- Playback ----------------------------------------------------------

    fun setResumeMode(mode: ResumeMode) = launch { playbackSettings.setResumeMode(mode) }

    fun setRememberBrightness(enabled: Boolean) =
        launch { playbackSettings.setRememberBrightness(enabled) }

    fun setRememberBackgroundPlay(enabled: Boolean) =
        launch { playbackSettings.setRememberBackgroundPlay(enabled) }

    fun setRememberAspectRatio(enabled: Boolean) =
        launch { playbackSettings.setRememberAspectRatio(enabled) }

    fun setRememberSpeed(enabled: Boolean) = launch { playbackSettings.setRememberSpeed(enabled) }

    fun setOrientationMode(mode: OrientationMode) =
        launch { playbackSettings.setOrientationMode(mode) }

    fun setSeekStepSeconds(seconds: Int) = launch { playbackSettings.setSeekStepSeconds(seconds) }

    fun setAutoplayNext(enabled: Boolean) = launch { playbackSettings.setAutoplayNext(enabled) }

    fun setAutoPip(enabled: Boolean) = launch { playbackSettings.setAutoPip(enabled) }

    fun setGesturesEnabled(enabled: Boolean) =
        launch { playbackSettings.setGesturesEnabled(enabled) }

    // --- Subtitles ---------------------------------------------------------

    fun setShowSubtitlesByDefault(enabled: Boolean) =
        launch { playbackSettings.setShowSubtitlesByDefault(enabled) }

    fun setPreferredAudioLanguage(tag: String?) =
        launch { playbackSettings.setPreferredAudioLanguage(tag) }

    fun setPreferredSubtitleLanguage(tag: String?) =
        launch { playbackSettings.setPreferredSubtitleLanguage(tag) }

    // --- Display / music ---------------------------------------------------

    fun setOverlayItem(item: OverlayItem, enabled: Boolean) =
        launch { playbackSettings.setOverlayItem(item, enabled) }

    fun setShowMusic(enabled: Boolean) = launch { playbackSettings.setShowMusic(enabled) }

    // --- File arrangement --------------------------------------------------

    fun setFileSortOrder(order: FileSortOrder) =
        launch { playbackSettings.setFileSortOrder(order) }

    fun setFolderSortOrder(order: FolderSortOrder) =
        launch { playbackSettings.setFolderSortOrder(order) }

    fun setFoldersFirst(enabled: Boolean) = launch { playbackSettings.setFoldersFirst(enabled) }

    // --- Storage -----------------------------------------------------------

    /** Drops the thumbnail and temporary caches, then re-reads the size. */
    fun clearCache() {
        viewModelScope.launch {
            val freed = cacheManager.clear()
            _cacheSize.value = cacheManager.cacheSizeBytes()
            _lastCleared.value = formatBytes(freed)
        }
    }

    /** Clears the "freed X" confirmation so it does not reappear on the next visit. */
    fun consumeClearResult() {
        _lastCleared.value = null
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
