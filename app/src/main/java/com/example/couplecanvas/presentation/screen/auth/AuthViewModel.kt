package com.example.couplecanvas.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couplecanvas.BuildConfig
import com.example.couplecanvas.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val user: StateFlow<FirebaseUser?> = authRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.currentUser)

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { authRepository.signInWithGoogle(idToken) }
                .onSuccess { _uiState.value = AuthUiState() }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Google 로그인에 실패했어요") }
        }
    }

    fun signInForDebugTest() {
        if (!BuildConfig.DEBUG || !BuildConfig.USE_FIREBASE_EMULATORS) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { authRepository.signInForDebugTest() }
                .onSuccess { _uiState.value = AuthUiState() }
                .onFailure {
                    _uiState.value = AuthUiState(
                        error = "Firebase Emulator 필요",
                    )
                }
        }
    }

    fun showError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
