package com.example.nplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.nplayer.data.media.MediaItem as MediaFile
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
import kotlinx.coroutines.withContext

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

    suspend fun play(item: MediaFile, subtitleUri: String? = null) {
        val startPosition = positionStore.position(item.key)

        val mediaItem = mediaItemFor(item, subtitleUri)

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
    suspend fun playQueue(items: List<MediaFile>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        val mediaItems = items.map { mediaItemFor(it, subtitleUri = null) }
        withContext(Dispatchers.Main) {
            val c = controller ?: return@withContext
            c.setMediaItems(mediaItems, startIndex.coerceIn(items.indices), 0L)
            c.prepare()
            c.play()
        }
        currentKey = items.getOrNull(startIndex)?.key
        startProgressLoop()
    }

    private fun mediaItemFor(item: MediaFile, subtitleUri: String?): MediaItem =
        MediaItem.Builder()
            .setMediaId(item.key)
            .setUri(item.uri)
            .setSubtitleConfigurations(buildList {
                if (subtitleUri != null) {
                    add(
                        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUri))
                            .setMimeType("application/x-subrip")
                            .setLanguage("und")
                            .build(),
                    )
                }
            })
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setSubtitle(item.artist ?: item.album ?: "")
                    .build(),
            )
            .build()

    /** Adds a sidecar subtitle file to the item that is currently playing. */
    fun addSubtitle(uri: String) {
        val c = controller ?: return
        val current = c.currentMediaItem ?: return
        val updated = current.buildUpon()
            .setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(uri))
                        .setMimeType("application/x-subrip")
                        .setLanguage("und")
                        .build(),
                ),
            )
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
