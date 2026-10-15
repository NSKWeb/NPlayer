package com.example.nplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nplayer.playback.ResizeMode
import com.example.nplayer.data.media.FileSortOrder
import com.example.nplayer.data.media.FolderSortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playbackDataStore by preferencesDataStore(name = "nplayer_playback")

/** What to do when an item has a saved position. */
enum class ResumeMode(val label: String) {
    ASK("Ask every time"),
    ALWAYS("Always resume"),
    NEVER("Start from the beginning"),
    ;

    companion object {
        fun fromStorage(raw: String?): ResumeMode =
            entries.firstOrNull { it.name == raw } ?: ASK
    }
}

/**
 * Which orientation the player forces when a video opens. [AUTO] follows the device
 * sensor, [VIDEO] follows the file's own aspect ratio, and the rest pin one way.
 */
enum class OrientationMode(val label: String) {
    AUTO("Auto rotate"),
    VIDEO("Video orientation"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    ;

    companion object {
        fun fromStorage(raw: String?): OrientationMode =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

/** How far the double-tap and button rewind/forward jumps move, in seconds. */
val SEEK_STEP_PRESETS = listOf(5, 10, 15, 30, 45, 60)

/** Overlay readouts that can be shown over the video while it plays. */
enum class OverlayItem(val label: String) {
    REMAINING_TIME("Remaining time"),
    CLOCK("Clock"),
    BATTERY("Battery percentage"),
}

/**
 * Playback preferences. Unlike [AppSettings], which holds things the whole app reads,
 * these are consulted by the player each time an item opens.
 */
data class PlaybackSettings(
    val resumeMode: ResumeMode = ResumeMode.ASK,
    val rememberBrightness: Boolean = false,
    val rememberBackgroundPlay: Boolean = false,
    val rememberAspectRatio: Boolean = false,
    val rememberSpeed: Boolean = false,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
    val seekStepSeconds: Int = 10,
    val autoplayNext: Boolean = true,
    val autoPip: Boolean = false,
    val overlayItems: Set<OverlayItem> = setOf(OverlayItem.REMAINING_TIME),
    val gesturesEnabled: Boolean = true,
    val showSubtitlesByDefault: Boolean = true,
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
    val showMusic: Boolean = true,
    /** How media lists arrange their files, and how folder lists arrange folders. */
    val fileSortOrder: FileSortOrder = FileSortOrder.DATE_NEWEST,
    val folderSortOrder: FolderSortOrder = FolderSortOrder.NAME_ASC,
    /** When on, folders are listed above files inside a folder. */
    val foldersFirst: Boolean = true,
    /** Remembered across items when the matching "remember" switch is on. */
    val lastBrightness: Float = 0.5f,
    val lastResizeMode: ResizeMode = ResizeMode.FIT,
    val lastSpeed: Float = 1f,
)

@Singleton
class PlaybackSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resumeModeKey = stringPreferencesKey("resume_mode")
    private val rememberBrightnessKey = booleanPreferencesKey("remember_brightness")
    private val rememberBackgroundKey = booleanPreferencesKey("remember_background")
    private val rememberAspectKey = booleanPreferencesKey("remember_aspect")
    private val rememberSpeedKey = booleanPreferencesKey("remember_speed")
    private val orientationKey = stringPreferencesKey("orientation_mode")
    private val seekStepKey = intPreferencesKey("seek_step_seconds")
    private val autoplayKey = booleanPreferencesKey("autoplay_next")
    private val autoPipKey = booleanPreferencesKey("auto_pip")
    private val overlayKey = stringPreferencesKey("overlay_items")
    private val gesturesKey = booleanPreferencesKey("gestures_enabled")
    private val subtitlesDefaultKey = booleanPreferencesKey("subtitles_default")
    private val audioLanguageKey = stringPreferencesKey("preferred_audio_language")
    private val subtitleLanguageKey = stringPreferencesKey("preferred_subtitle_language")
    private val showMusicKey = booleanPreferencesKey("show_music")
    private val fileSortKey = stringPreferencesKey("file_sort_order")
    private val folderSortKey = stringPreferencesKey("folder_sort_order")
    private val foldersFirstKey = booleanPreferencesKey("folders_first")
    private val lastBrightnessKey = floatPreferencesKey("last_brightness")
    private val lastResizeKey = stringPreferencesKey("last_resize_mode")
    private val lastSpeedKey = floatPreferencesKey("last_speed")

    val settings: Flow<PlaybackSettings> = context.playbackDataStore.data.map { prefs ->
        val defaults = PlaybackSettings()
        PlaybackSettings(
            resumeMode = ResumeMode.fromStorage(prefs[resumeModeKey]),
            rememberBrightness = prefs[rememberBrightnessKey] ?: defaults.rememberBrightness,
            rememberBackgroundPlay = prefs[rememberBackgroundKey]
                ?: defaults.rememberBackgroundPlay,
            rememberAspectRatio = prefs[rememberAspectKey] ?: defaults.rememberAspectRatio,
            rememberSpeed = prefs[rememberSpeedKey] ?: defaults.rememberSpeed,
            orientationMode = OrientationMode.fromStorage(prefs[orientationKey]),
            seekStepSeconds = prefs[seekStepKey] ?: defaults.seekStepSeconds,
            autoplayNext = prefs[autoplayKey] ?: defaults.autoplayNext,
            autoPip = prefs[autoPipKey] ?: defaults.autoPip,
            overlayItems = decodeOverlay(prefs[overlayKey], defaults.overlayItems),
            gesturesEnabled = prefs[gesturesKey] ?: defaults.gesturesEnabled,
            showSubtitlesByDefault = prefs[subtitlesDefaultKey]
                ?: defaults.showSubtitlesByDefault,
            preferredAudioLanguage = prefs[audioLanguageKey],
            preferredSubtitleLanguage = prefs[subtitleLanguageKey],
            showMusic = prefs[showMusicKey] ?: defaults.showMusic,
            fileSortOrder = FileSortOrder.fromStorage(prefs[fileSortKey]),
            folderSortOrder = FolderSortOrder.fromStorage(prefs[folderSortKey]),
            foldersFirst = prefs[foldersFirstKey] ?: defaults.foldersFirst,
            lastBrightness = prefs[lastBrightnessKey] ?: defaults.lastBrightness,
            lastResizeMode = prefs[lastResizeKey]
                ?.let { raw -> ResizeMode.entries.firstOrNull { it.name == raw } }
                ?: defaults.lastResizeMode,
            lastSpeed = prefs[lastSpeedKey] ?: defaults.lastSpeed,
        )
    }

    suspend fun current(): PlaybackSettings = settings.first()

    suspend fun setResumeMode(mode: ResumeMode) = put(resumeModeKey, mode.name)

    suspend fun setRememberBrightness(enabled: Boolean) = put(rememberBrightnessKey, enabled)

    suspend fun setRememberBackgroundPlay(enabled: Boolean) =
        put(rememberBackgroundKey, enabled)

    suspend fun setRememberAspectRatio(enabled: Boolean) = put(rememberAspectKey, enabled)

    suspend fun setRememberSpeed(enabled: Boolean) = put(rememberSpeedKey, enabled)

    suspend fun setOrientationMode(mode: OrientationMode) = put(orientationKey, mode.name)

    suspend fun setSeekStepSeconds(seconds: Int) = put(seekStepKey, seconds)

    suspend fun setAutoplayNext(enabled: Boolean) = put(autoplayKey, enabled)

    suspend fun setAutoPip(enabled: Boolean) = put(autoPipKey, enabled)

    suspend fun setGesturesEnabled(enabled: Boolean) = put(gesturesKey, enabled)

    suspend fun setShowSubtitlesByDefault(enabled: Boolean) = put(subtitlesDefaultKey, enabled)

    suspend fun setPreferredAudioLanguage(tag: String?) = putOrClear(audioLanguageKey, tag)

    suspend fun setPreferredSubtitleLanguage(tag: String?) = putOrClear(subtitleLanguageKey, tag)

    suspend fun setShowMusic(enabled: Boolean) = put(showMusicKey, enabled)

    suspend fun setFileSortOrder(order: FileSortOrder) = put(fileSortKey, order.name)

    suspend fun setFolderSortOrder(order: FolderSortOrder) = put(folderSortKey, order.name)

    suspend fun setFoldersFirst(enabled: Boolean) = put(foldersFirstKey, enabled)

    suspend fun setOverlayItem(item: OverlayItem, enabled: Boolean) {
        context.playbackDataStore.edit { prefs ->
            val current = decodeOverlay(prefs[overlayKey], PlaybackSettings().overlayItems)
            val updated = if (enabled) current + item else current - item
            prefs[overlayKey] = updated.joinToString(",") { it.name }
        }
    }

    /** Records the values the "remember" switches carry between items. */
    suspend fun rememberBrightness(value: Float) = put(lastBrightnessKey, value)

    suspend fun rememberResizeMode(mode: ResizeMode) = put(lastResizeKey, mode.name)

    suspend fun rememberSpeed(speed: Float) = put(lastSpeedKey, speed)

    private suspend fun <T> put(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.playbackDataStore.edit { it[key] = value }
    }

    private suspend fun putOrClear(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String?) {
        context.playbackDataStore.edit { prefs ->
            if (value.isNullOrBlank()) prefs.remove(key) else prefs[key] = value
        }
    }
}

/** Overlay choices are stored as a comma-joined name list; unknown names are dropped. */
private fun decodeOverlay(raw: String?, fallback: Set<OverlayItem>): Set<OverlayItem> {
    if (raw == null) return fallback
    val parsed = raw.split(',')
        .mapNotNull { name -> OverlayItem.entries.firstOrNull { it.name == name } }
        .toSet()
    return parsed
}
