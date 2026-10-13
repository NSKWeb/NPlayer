package com.example.nplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.prefs.SessionStore
import com.example.nplayer.data.prefs.ThemeMode
import com.example.nplayer.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String? = null,
    val userName: String? = null,
    val biometricLock: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = sessionStore.session
        .map {
            SettingsUiState(
                serverUrl = it.serverUrl,
                userName = it.userName,
                biometricLock = it.biometricLockEnabled,
                themeMode = it.themeMode,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { sessionStore.setBiometricLock(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { sessionStore.setThemeMode(mode) }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
