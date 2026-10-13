package com.example.nplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "nplayer_session")

/** Connection + session state that must survive process death. */
data class Session(
    val serverUrl: String? = null,
    val accessToken: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val deviceId: String,
    val biometricLockEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val isAuthenticated: Boolean
        get() = !serverUrl.isNullOrBlank() && !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
}

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
    deviceIdProvider: DeviceIdProvider,
) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("access_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val userNameKey = stringPreferencesKey("user_name")
    private val biometricKey = booleanPreferencesKey("biometric_lock")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    private val fallbackDeviceId = deviceIdProvider.deviceId

    val session: Flow<Session> = context.dataStore.data.map { prefs ->
        Session(
            serverUrl = prefs[serverUrlKey],
            accessToken = prefs[tokenKey],
            userId = prefs[userIdKey],
            userName = prefs[userNameKey],
            deviceId = fallbackDeviceId,
            biometricLockEnabled = prefs[biometricKey] ?: false,
            themeMode = ThemeMode.fromStorage(prefs[themeModeKey]),
        )
    }

    suspend fun current(): Session = session.first()

    /** Persists only the server host so requests can be routed before login. */
    suspend fun saveServerUrlForProbe(serverUrl: String) {
        context.dataStore.edit { it[serverUrlKey] = serverUrl.trimEnd('/') }
    }

    suspend fun saveLogin(serverUrl: String, accessToken: String, userId: String, userName: String) {
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = serverUrl.trimEnd('/')
            prefs[tokenKey] = accessToken
            prefs[userIdKey] = userId
            prefs[userNameKey] = userName
        }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[biometricKey] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(userIdKey)
            prefs.remove(userNameKey)
        }
    }
}
