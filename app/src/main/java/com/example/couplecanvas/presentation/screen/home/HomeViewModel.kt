package com.example.couplecanvas.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couplecanvas.data.model.JoinRoomResult
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.RoomHomeSummary
import com.example.couplecanvas.data.repository.AuthRepository
import com.example.couplecanvas.data.repository.RoomRepository
import com.example.couplecanvas.util.RoomCodeGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val rooms: List<Room> = emptyList(),
    val roomSummaries: List<RoomHomeSummary> = emptyList(),
    val displayName: String = "Google 계정",
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isFirebaseConnected: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val createdRoomId: String? = null,
    val joinedRoomId: String? = null,
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var roomsJob: Job? = null

    init {
        observeConnection()
        observeRooms()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            roomRepository.observeFirebaseConnection().collect { connected ->
                _uiState.value = _uiState.value.copy(isFirebaseConnected = connected)
            }
        }
    }

    private fun observeRooms() {
        val uid = authRepository.currentUser?.uid ?: return
        val user = authRepository.currentUser
        _uiState.value = _uiState.value.copy(displayName = user?.displayName ?: user?.email ?: "Google 계정")
        roomsJob?.cancel()
        roomsJob = viewModelScope.launch {
            roomRepository.observeRoomSummariesForUser(uid).collect { summaries ->
                _uiState.value = _uiState.value.copy(
                    rooms = summaries.map { it.room },
                    roomSummaries = summaries,
                    isLoading = false,
                )
            }
        }
    }

    fun createRoom(title: String = "둘만의 그림방") {
        val uid = authRepository.currentUser?.uid ?: return
        val roomTitle = title.trim().takeIf { it.isNotEmpty() } ?: "둘만의 그림방"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null, createdRoomId = null)
            runCatching { roomRepository.createRoom(uid, title = roomTitle) }
                .onSuccess { _uiState.value = _uiState.value.copy(isBusy = false, createdRoomId = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isBusy = false, error = it.message ?: "방 만들기에 실패했어요") }
        }
    }

    fun joinRoom(code: String) {
        val uid = authRepository.currentUser?.uid ?: return
        val normalized = code.trim().uppercase()
        if (!RoomCodeGenerator.isValid(normalized)) {
            _uiState.value = _uiState.value.copy(error = "6자리 코드를 확인해주세요", message = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null, joinedRoomId = null)
            when (val result = roomRepository.joinRoom(normalized, uid)) {
                is JoinRoomResult.Success -> _uiState.value = _uiState.value.copy(isBusy = false, joinedRoomId = result.roomId)
                JoinRoomResult.NotFound -> _uiState.value = _uiState.value.copy(isBusy = false, error = "없는 코드예요")
                JoinRoomResult.Full -> _uiState.value = _uiState.value.copy(isBusy = false, error = "이미 가득 찬 방이에요")
                JoinRoomResult.Closed -> _uiState.value = _uiState.value.copy(isBusy = false, error = "보관된 방이에요")
                JoinRoomResult.AlreadyMember -> _uiState.value = _uiState.value.copy(isBusy = false)
                is JoinRoomResult.Error -> _uiState.value = _uiState.value.copy(isBusy = false, error = result.message)
            }
        }
    }

    fun closeRoom(roomId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        launchBusy(successMessage = "방을 보관했어요") {
            roomRepository.closeRoom(roomId, uid)
        }
    }

    fun reopenRoom(roomId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        launchBusy(successMessage = "방을 다시 열었어요") {
            roomRepository.reopenRoom(roomId, uid)
        }
    }

    fun leaveRoom(roomId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        launchBusy(successMessage = "내 목록에서 방을 나갔어요") {
            roomRepository.leaveRoom(roomId, uid)
        }
    }

    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(createdRoomId = null, joinedRoomId = null)
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private fun launchBusy(successMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null)
            runCatching { block() }
                .onSuccess { _uiState.value = _uiState.value.copy(isBusy = false, message = successMessage) }
                .onFailure { _uiState.value = _uiState.value.copy(isBusy = false, error = it.message ?: "작업에 실패했어요") }
        }
    }
}
