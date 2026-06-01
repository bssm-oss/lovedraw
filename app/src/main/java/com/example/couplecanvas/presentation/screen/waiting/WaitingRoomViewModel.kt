package com.example.couplecanvas.presentation.screen.waiting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.repository.AuthRepository
import com.example.couplecanvas.data.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WaitingUiState(
    val room: Room? = null,
    val partnerJoined: Boolean = false,
    val isFirebaseConnected: Boolean = false,
    val error: String? = null,
)

class WaitingRoomViewModel(
    private val roomId: String,
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WaitingUiState())
    val uiState: StateFlow<WaitingUiState> = _uiState

    init {
        viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { room ->
                val uid = authRepository.currentUser?.uid
                val partnerJoined = room?.members.orEmpty().keys.any { it != uid }
                _uiState.update { current ->
                    current.copy(room = room, partnerJoined = partnerJoined, error = null)
                }
            }
        }
        viewModelScope.launch {
            roomRepository.observeFirebaseConnection().collect { connected ->
                _uiState.update { current -> current.copy(isFirebaseConnected = connected) }
            }
        }
    }

    fun leave() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch { roomRepository.leaveRoom(roomId, uid) }
    }
}
