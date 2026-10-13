package com.example.nplayer.data.remote

import com.example.nplayer.data.prefs.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coil fetches image URLs directly, bypassing Retrofit, so we inject the
 * Jellyfin auth header here for media/image requests.
 */
@Singleton
class JellyfinImageInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { sessionStore.current() }
        val request = chain.request()
        val builder = request.newBuilder()
        session.accessToken?.let { builder.header("Authorization", "MediaBrowser Token=\"$it\"") }
        builder.header("X-Emby-Authorization", buildAuthHeader(session))
        return chain.proceed(builder.build())
    }
}

internal fun buildAuthHeader(session: com.example.nplayer.data.prefs.Session): String =
    buildString {
        append("MediaBrowser Client=\"N Player\", Device=\"Android\", DeviceId=\"")
        append(session.deviceId)
        append("\", Version=\"1.0.0\"")
    }
