package com.example.nplayer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nplayer.data.repository.ApiResult
import com.example.nplayer.data.repository.AppError
import com.example.nplayer.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onServerUrlChange(value: String) = _state.update { it.copy(serverUrl = value, error = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val current = _state.value
        if (current.serverUrl.isBlank() || current.username.isBlank()) {
            _state.update { it.copy(error = "Server URL and username are required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val normalized = normalizeUrl(current.serverUrl)
            when (val result = authRepository.login(normalized, current.username, current.password)) {
                is ApiResult.Success -> _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = messageFor(result.error))
                }
            }
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://$trimmed"
    }

    private fun messageFor(error: AppError): String = when (error) {
        AppError.NETWORK -> "Could not reach the server. Check the URL and your connection."
        AppError.UNAUTHORIZED -> "Invalid username or password."
        AppError.SERVER -> "The server returned an error."
        AppError.UNKNOWN -> "Sign-in failed."
    }
}
