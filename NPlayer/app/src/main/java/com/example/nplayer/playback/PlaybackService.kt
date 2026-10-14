package com.example.nplayer.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground media service owning the [ExoPlayer] instance and a
 * [MediaSession], which provides the media notification, lock-screen controls,
 * audio focus, and background playback.
 */
@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var equalizerController: EqualizerController

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    /**
     * Ends the service once there is nothing left to play, so a finished or idle
     * player does not keep the process and its notification alive in the background.
     * Playback that is still running is deliberately left alone: background audio is
     * the point of the service.
     */
    private val idleStopListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val p = player ?: return
                if (!p.hasNextMediaItem() && p.repeatMode == Player.REPEAT_MODE_OFF) {
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Local files are read through DefaultDataSource; there is no network source.
        val dataSourceFactory = DefaultDataSource.Factory(this)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(idleStopListener)
        player = exoPlayer

        // The equalizer binds to the player's audio session, which only exists
        // here, in the service that owns the ExoPlayer instance.
        exoPlayer.audioSessionId.takeIf { it != 0 }?.let(equalizerController::attach)

        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Called when the user swipes the app out of recents. Playback ends here too, so
     * dismissing the app never leaves it running (or holding a notification) in the
     * background.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.run {
            stop()
            clearMediaItems()
        }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(idleStopListener)
            player.release()
            release()
        }
        mediaSession = null
        player = null
        // The session is gone with the service, so the equalizer must rebind next time.
        equalizerController.release()
        super.onDestroy()
    }
}
