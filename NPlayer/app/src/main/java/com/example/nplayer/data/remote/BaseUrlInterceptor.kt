package com.example.nplayer.data.remote

import com.example.nplayer.data.prefs.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit is built once with a placeholder base URL; the real server host is
 * chosen by the user at runtime. This interceptor rewrites each request onto the
 * configured server origin while preserving path + query.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val serverUrl = runBlocking { sessionStore.current().serverUrl }
            ?: return chain.proceed(request)

        val origin = serverUrl.toHttpUrlOrNull() ?: return chain.proceed(request)
        val newUrl = request.url.newBuilder()
            .scheme(origin.scheme)
            .host(origin.host)
            .port(origin.port)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
