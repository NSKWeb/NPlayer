package com.example.nplayer.playback

import com.example.nplayer.data.prefs.SessionStore
import com.example.nplayer.data.remote.model.MediaSourceInfo
import javax.inject.Inject
import javax.inject.Singleton

/** Session-aware façade over [StreamUrls]; resolves the server + token first. */
@Singleton
class StreamUrlBuilder @Inject constructor(
    private val sessionStore: SessionStore,
) {
    private suspend fun serverAndToken(): Pair<String, String> {
        val session = sessionStore.current()
        val server = session.serverUrl?.trimEnd('/') ?: error("No server configured")
        val token = session.accessToken ?: error("Not authenticated")
        return server to token
    }

    suspend fun build(itemId: String, mediaSource: MediaSourceInfo?): String {
        val (server, token) = serverAndToken()
        return StreamUrls.streamUrl(server, token, itemId, mediaSource)
    }

    suspend fun subtitleUrl(itemId: String, streamIndex: Int, mediaId: String?): String {
        val (server, token) = serverAndToken()
        return StreamUrls.subtitleUrl(server, token, itemId, mediaId ?: itemId, streamIndex)
    }

    suspend fun imageUrl(itemId: String, tag: String?): String? {
        val session = sessionStore.current()
        val server = session.serverUrl?.trimEnd('/') ?: return null
        return StreamUrls.imageUrl(server, itemId, tag)
    }
}
