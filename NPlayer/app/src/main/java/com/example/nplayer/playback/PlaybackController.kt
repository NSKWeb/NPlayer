package com.example.nplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.nplayer.data.media.MediaItem as MediaFile
import com.example.nplayer.data.media.SubtitleFile
import com.example.nplayer.data.media.languageDisplayName
import com.example.nplayer.data.media.sidecarLabel
import com.example.nplayer.data.media.sidecarLanguageTag
import com.example.nplayer.data.media.sidecarMimeType
import com.example.nplayer.data.prefs.PlaybackPositionStore
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * UI-facing wrapper around the Media3 [MediaController]. Plays a device file by its
 * `content://` URI and restores the resume position. There is no server to report
 * progress to, so positions stay on the device.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val positionStore: PlaybackPositionStore,
) {
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var currentKey: String? = null

    fun connect(): ListenableFuture<MediaController> {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
        return future
    }

    /**
     * Connects and suspends until the controller is ready. Never block the main
     * thread on [connect]'s future: binding the service needs the main thread to
     * complete, so `.get()` there deadlocks and playback never starts.
     */
    suspend fun connectAndAwait(): MediaController = connect().await()

    suspend fun play(item: MediaFile, subtitleUri: String? = null, sidecars: List<SubtitleFile> = emptyList()) {
        val startPosition = positionStore.position(item.key)

        val mediaItem = mediaItemFor(item, subtitleUri, sidecars)

        withContext(Dispatchers.Main) {
            val c = controller ?: return@withContext
            c.setMediaItem(mediaItem, startPosition)
            c.prepare()
            c.play()
        }
        currentKey = item.key
        startProgressLoop()
    }

    /** Plays [items] as one queue, starting at [startIndex], like a playlist. */
    suspend fun playQueue(
        items: List<MediaFile>,
        startIndex: Int = 0,
        sidecars: (MediaFile) -> List<SubtitleFile> = { emptyList() },
    ) {
        if (items.isEmpty()) return
        val mediaItems = items.map { mediaItemFor(it, subtitleUri = null, sidecars = sidecars(it)) }
        withContext(Dispatchers.Main) {
            val c = controller ?: return@withContext
            c.setMediaItems(mediaItems, startIndex.coerceIn(items.indices), 0L)
            c.prepare()
            c.play()
        }
        currentKey = items.getOrNull(startIndex)?.key
        startProgressLoop()
    }

    private fun mediaItemFor(
        item: MediaFile,
        subtitleUri: String?,
        sidecars: List<SubtitleFile>,
    ): MediaItem {
        // Every sidecar sitting next to the file is offered as a track, so the
        // subtitle button can list them without a second scan.
        val configurations = sidecars.map { sidecar ->
            MediaItem.SubtitleConfiguration.Builder(sidecar.uri)
                .setMimeType(sidecarMimeType(sidecar.name.substringAfterLast('.', "")) ?: "application/x-subrip")
                .setLanguage(sidecarLanguageTag(sidecar.name) ?: "und")
                .setLabel(sidecarLabel(item.displayName, sidecar.name))
                .setId(sidecar.name)
                .build()
        }.toMutableList()
        // A file the user picked by hand, when it is not already in the list.
        if (subtitleUri != null && configurations.none { it.uri.toString() == subtitleUri }) {
            configurations += MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUri))
                .setMimeType("application/x-subrip")
                .setLanguage("und")
                .setLabel("Picked file")
                .setId(subtitleUri)
                .build()
        }

        return MediaItem.Builder()
            .setMediaId(item.key)
            .setUri(item.uri)
            .setSubtitleConfigurations(configurations)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setSubtitle(item.artist ?: item.album ?: "")
                    .build(),
            )
            .build()
    }

    /**
     * The subtitle tracks on the item that is playing: sidecar files first, then text
     * tracks muxed into the container.
     */
    fun subtitleTracks(): List<SubtitleTrack> {
        val c = controller ?: return emptyList()
        val current = c.currentMediaItem ?: return emptyList()

        val sidecars = current.localConfiguration?.subtitleConfigurations.orEmpty().map { config ->
            val name = config.id ?: config.uri.lastPathSegment.orEmpty()
            SubtitleTrack(
                id = name,
                label = config.label?.toString() ?: name,
                language = config.language,
                mimeType = config.mimeType,
            )
        }

        val embedded = c.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .mapIndexed { index, group ->
                val format = group.getTrackFormat(0)
                val language = languageDisplayName(format.language)
                SubtitleTrack(
                    id = group.mediaTrackGroup.id,
                    label = language ?: format.label ?: "Track ${index + 1}",
                    language = format.language,
                    mimeType = format.sampleMimeType,
                    embedded = true,
                )
            }

        return sidecars + embedded
    }

    /** The subtitle track currently rendering, or null when subtitles are off. */
    fun selectedSubtitleTrack(): SubtitleTrack? {
        val c = controller ?: return null
        val selectedGroup = c.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_TEXT && it.isTrackSelected(0) }
            ?: return null
        val tracks = subtitleTracks()
        // An embedded track carries its group id, so it matches exactly.
        tracks.firstOrNull { it.embedded && it.id == selectedGroup.mediaTrackGroup.id }
            ?.let { return it }
        // A sidecar's group id is not stable across Media3 versions, so a selected
        // text track that is not embedded is taken to be the (single) sidecar.
        return tracks.firstOrNull { it.isExternal }
    }

    /**
     * Shows [track], or turns subtitles off when it is null. Only one subtitle is
     * shown at a time: selecting a track clears any other text override.
     */
    fun selectSubtitle(track: SubtitleTrack?) {
        val c = controller ?: return
        val builder = c.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)

        if (track == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            // A sidecar and an embedded track are both addressed by their group id, and
            // only one text track is shown at a time, so one override is enough.
            c.currentTracks.groups
                .firstOrNull { it.type == C.TRACK_TYPE_TEXT && it.mediaTrackGroup.id == track.id }
                ?.let { builder.setOverrideForType(TrackSelectionOverride(it.mediaTrackGroup, 0)) }
        }
        c.trackSelectionParameters = builder.build()
    }

    /** The text Media3 is currently rendering, or null when no caption is showing. */
    fun currentCueText(): String? {
        val cues = controller?.currentCues?.cues.orEmpty()
        val text = cues.mapNotNull { it.text?.toString()?.takeIf(String::isNotBlank) }
            .joinToString("\n")
        return text.ifBlank { null }
    }

    /**
     * Appends a subtitle file the user picked by hand to the item that is playing, and
     * shows it. Replacing the item keeps the current position, so playback is not
     * interrupted.
     */
    fun attachPickedSubtitle(uri: String) {
        val c = controller ?: return
        val current = c.currentMediaItem ?: return
        val existing = current.localConfiguration?.subtitleConfigurations.orEmpty()
        if (existing.any { it.uri.toString() == uri }) {
            existing.firstOrNull { it.uri.toString() == uri }
                ?.let { selectSubtitle(subtitleTracks().firstOrNull { track -> track.id == it.id }) }
            return
        }
        val configuration = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(uri))
            .setMimeType("application/x-subrip")
            .setLanguage("und")
            .setLabel("Picked file")
            .setId(uri)
            .build()
        val updated = current.buildUpon()
            .setSubtitleConfigurations(existing + configuration)
            .build()
        c.replaceMediaItem(c.currentMediaItemIndex, updated)
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                controller?.let { c ->
                    // In a queue the current item advances, so save against whichever
                    // item is playing right now rather than the one that started it.
                    val key = c.currentMediaItem?.mediaId
                    if (key != null) positionStore.save(key, c.currentPosition)
                }
                delay(5_000)
            }
        }
    }

    suspend fun stop(itemKey: String) {
        progressJob?.cancel()
        val position = withContext(Dispatchers.Main) { controller?.currentPosition ?: 0L }
        positionStore.save(itemKey, position)
        currentKey = null
    }

    fun release() {
        progressJob?.cancel()
        controller?.release()
        controller = null
        currentKey = null
    }
}

/** Suspends until [this] future completes, without blocking any thread. */
private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    addListener(
        {
            runCatching { get() }
                .onSuccess { cont.resume(it) }
                .onFailure { cont.resumeWithException(it) }
        },
        MoreExecutors.directExecutor(),
    )
}
