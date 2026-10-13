package com.example.nplayer.data.repository

import com.example.nplayer.data.prefs.Session
import com.example.nplayer.data.prefs.SessionStore
import com.example.nplayer.data.remote.JellyfinApi
import com.example.nplayer.data.remote.model.AuthenticationRequest
import com.example.nplayer.data.remote.model.PublicSystemInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: JellyfinApi,
    private val sessionStore: SessionStore,
) {
    val session: Flow<Session> = sessionStore.session

    suspend fun currentSession(): Session = sessionStore.current()

    /** Validates a server URL and returns its public system info. */
    suspend fun probeServer(serverUrl: String): ApiResult<PublicSystemInfo> = apiCall {
        // The BaseUrlInterceptor reads the stored URL, so persist it first (without token).
        sessionStore.saveServerUrlForProbe(serverUrl)
        api.publicSystemInfo()
    }

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
    ): ApiResult<Session> = apiCall {
        sessionStore.saveServerUrlForProbe(serverUrl)
        val result = api.authenticateByName(AuthenticationRequest(username = username, password = password))
        sessionStore.saveLogin(
            serverUrl = serverUrl,
            accessToken = result.accessToken,
            userId = result.user.id,
            userName = result.user.name,
        )
        sessionStore.current()
    }

    suspend fun logout() {
        sessionStore.clear()
    }
}
