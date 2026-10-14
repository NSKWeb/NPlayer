package com.example.nplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.prefs.AppSettingsStore
import com.example.nplayer.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val biometricLock: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val favouriteCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = settingsStore.settings
        .map {
            SettingsUiState(
                biometricLock = it.biometricLockEnabled,
                themeMode = it.themeMode,
                dynamicColor = it.dynamicColor,
                favouriteCount = it.favourites.size,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setBiometricLock(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }
}
