package com.example.nplayer.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.nplayer.data.local.PlaybackPositionStore
import com.example.nplayer.data.remote.JellyfinApi
import com.example.nplayer.data.remote.model.BaseItemDto
import com.example.nplayer.data.remote.model.PlaybackProgressInfo
import com.example.nplayer.data.remote.model.PlaybackStopInfo
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-facing wrapper around the Media3 [MediaController]. Resolves an item to a
 * playable [MediaItem], restores resume position, and reports progress back to
 * the Jellyfin server so "Continue watching" stays in sync across devices.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamUrlBuilder: StreamUrlBuilder,
    private val positionStore: PlaybackPositionStore,
    private val api: JellyfinApi,
) {
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: kotlinx.coroutines.Job? = null

    fun connect(): ListenableFuture<MediaController> {
        val token = SessionToken(context, android.content.ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
        return future
    }

    suspend fun play(item: BaseItemDto) {
        val mediaSource = item.mediaSources?.firstOrNull()
        val url = streamUrlBuilder.build(item.id, mediaSource)
        val startPosition = positionStore.position(item.id)

        val mediaItem = MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.name ?: "")
                    .setSubtitle(item.seriesName ?: item.type.orEmpty())
                    .build(),
            )
            .build()

        withContext(Dispatchers.Main) {
            val c = controller ?: return@withContext
            c.setMediaItem(mediaItem, startPosition)
            c.prepare()
            c.play()
        }
        reportStart(item.id)
        startProgressLoop(item.id)
    }

    private fun reportStart(itemId: String) {
        scope.launch {
            runCatching {
                api.reportPlaybackStart(PlaybackProgressInfo(itemId = itemId, canSeek = true))
            }
        }
    }

    private fun startProgressLoop(itemId: String) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    val position = c.currentPosition
                    positionStore.save(itemId, position)
                    runCatching {
                        api.reportPlaybackProgress(
                            PlaybackProgressInfo(
                                itemId = itemId,
                                positionTicks = position * 10_000,
                                isPaused = !c.isPlaying,
                            ),
                        )
                    }
                }
                delay(10_000)
            }
        }
    }

    suspend fun stop(itemId: String) {
        progressJob?.cancel()
        val position = withContext(Dispatchers.Main) { controller?.currentPosition ?: 0L }
        positionStore.save(itemId, position)
        runCatching {
            api.reportPlaybackStopped(
                PlaybackStopInfo(itemId = itemId, positionTicks = position * 10_000),
            )
        }
    }

    fun release() {
        progressJob?.cancel()
        controller?.release()
        controller = null
    }
}
