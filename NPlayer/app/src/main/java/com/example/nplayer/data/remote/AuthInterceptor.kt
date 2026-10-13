package com.example.nplayer.data.remote

import com.example.nplayer.data.prefs.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the Jellyfin authorization header to every API call. The token is read
 * from [SessionStore] at request time so it always reflects the latest login.
 *
 * Jellyfin's header is the same whether or not a token exists yet; unauthenticated
 * calls (public info, AuthenticateByName) simply omit the Token parameter.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { sessionStore.current() }
        val header = buildString {
            append("MediaBrowser Client=\"N Player\", Device=\"Android\", DeviceId=\"")
            append(session.deviceId)
            append("\", Version=\"1.0.0\"")
            session.accessToken?.takeIf { it.isNotBlank() }?.let {
                append(", Token=\"$it\"")
            }
        }
        val request = chain.request().newBuilder()
            .header("X-Emby-Authorization", header)
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
