package com.example.nplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "nplayer_settings")

/** User preferences. There is no account or server state to keep. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val biometricLockEnabled: Boolean = false,
    /** When true, the wallpaper palette replaces the app's own colours. */
    val dynamicColor: Boolean = false,
    val favourites: Set<String> = emptySet(),
    /** Items the user has moved into the private folder; hidden from every list. */
    val privateItems: Set<String> = emptySet(),
)

@Singleton
class AppSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val biometricKey = booleanPreferencesKey("biometric_lock")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val favouritesKey = stringSetPreferencesKey("favourites")
    private val privateItemsKey = stringSetPreferencesKey("private_items")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromStorage(prefs[themeModeKey]),
            biometricLockEnabled = prefs[biometricKey] ?: false,
            dynamicColor = prefs[dynamicColorKey] ?: false,
            favourites = prefs[favouritesKey] ?: emptySet(),
            privateItems = prefs[privateItemsKey] ?: emptySet(),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[biometricKey] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = enabled }
    }

    /** Adds or removes [key], returning the new favourite set. */
    suspend fun toggleFavourite(key: String): Set<String> {
        var updated = emptySet<String>()
        context.dataStore.edit { prefs ->
            val current = prefs[favouritesKey] ?: emptySet()
            updated = if (key in current) current - key else current + key
            prefs[favouritesKey] = updated
        }
        return updated
    }

    /** Adds or removes [key] from the private folder, returning the new set. */
    suspend fun togglePrivate(key: String): Set<String> {
        var updated = emptySet<String>()
        context.dataStore.edit { prefs ->
            val current = prefs[privateItemsKey] ?: emptySet()
            updated = if (key in current) current - key else current + key
            prefs[privateItemsKey] = updated
        }
        return updated
    }
}
