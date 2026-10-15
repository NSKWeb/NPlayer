package com.example.nplayer

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nplayer.ui.BiometricGate
import com.example.nplayer.ui.navigation.NPlayerApp
import com.example.nplayer.ui.player.rememberPlayerActive
import com.example.nplayer.ui.player.resetOrientation
import com.example.nplayer.ui.settings.SettingsViewModel
import com.example.nplayer.ui.theme.NPlayerTheme
import com.example.nplayer.ui.theme.resolveDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    /** The shape of the PiP window; updated from the video that is playing. */
    private var pipWidth = 16
    private var pipHeight = 9

    /** Set by the player screen: whether it is open, and whether auto-PiP is on. */
    private var playerScreenActive = false
    private var autoPipEnabled = false

    /**
     * Matches the PiP window to the video being played. A ratio outside the range
     * the framework accepts is clamped, because [PictureInPictureParams.Builder.setAspectRatio]
     * throws on an out-of-range value.
     */
    fun updatePictureInPictureAspectRatio(width: Int, height: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (width <= 0 || height <= 0) return
        if (width == pipWidth && height == pipHeight) return
        pipWidth = width
        pipHeight = height
        if (isInPictureInPictureMode) setPictureInPictureParams(pictureInPictureParams())
    }

    /** Enters picture-in-picture, sizing the window to the video's aspect ratio. */
    fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        enterPictureInPictureMode(pictureInPictureParams())
    }

    /**
     * Shrinks to the floating window when the viewer leaves the app mid-video, but only
     * when the auto-PiP setting is on and the player screen is the one being left.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!autoPipEnabled) return
        if (!playerScreenActive) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (isInPictureInPictureMode) return
        enterPictureInPictureMode(pictureInPictureParams())
    }

    /** Lets the composition report the auto-PiP setting and whether the player is open. */
    fun setPlayerState(playerActive: Boolean, autoPip: Boolean) {
        playerScreenActive = playerActive
        autoPipEnabled = autoPip
    }

    private fun pictureInPictureParams(): PictureInPictureParams {
        val ratio = (pipWidth.toFloat() / pipHeight.toFloat()).coerceIn(0.42f, 2.39f)
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()

            NPlayerTheme(
                darkTheme = settings.themeMode.resolveDark(),
                dynamicColor = settings.dynamicColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BiometricGate(enabled = settings.biometricLock, activity = this) {
                        NPlayerApp()
                    }
                }
            }
        }
    }
}
