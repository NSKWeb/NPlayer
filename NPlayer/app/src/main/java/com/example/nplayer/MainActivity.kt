package com.example.nplayer

import android.os.Bundle
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
import com.example.nplayer.ui.settings.SettingsViewModel
import com.example.nplayer.ui.theme.NPlayerTheme
import com.example.nplayer.ui.theme.resolveDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()

            NPlayerTheme(darkTheme = settings.themeMode.resolveDark()) {
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
