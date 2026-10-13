package com.example.nplayer.data.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A stable, app-scoped device identifier. Jellyfin uses this to bind sessions
 * and to let users revoke individual devices from their server.
 *
 * This is the platform's own per-app-android-id (resettable by the user), not a
 * spoofed or randomised hardware identity.
 */
@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val deviceId: String
        @SuppressLint("HardwareIds")
        get() {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID,
            )
            return if (androidId.isNullOrBlank()) {
                UUID.randomUUID().toString()
            } else {
                "nplayer-$androidId"
            }
        }
}
