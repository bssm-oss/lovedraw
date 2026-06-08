package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.RoomStatus

enum class ConnectionDisplayState(
    val label: String,
    val description: String,
) {
    Connected(
        label = "연결됨",
        description = "서로 같은 방에 있어요.",
    ),
    Waiting(
        label = "상대 대기 중",
        description = "초대 코드로 들어오면 바로 연결돼요.",
    ),
    Reconnecting(
        label = "재연결 중",
        description = "네트워크를 다시 확인하고 있어요.",
    ),
    Archived(
        label = "보관됨",
        description = "다시 열면 이어서 사용할 수 있어요.",
    ),
}

fun Room.connectionDisplayState(firebaseConnected: Boolean): ConnectionDisplayState =
    when {
        status == RoomStatus.Closed.value -> ConnectionDisplayState.Archived
        !firebaseConnected -> ConnectionDisplayState.Reconnecting
        guestUid.isNullOrBlank() && activeUserCount < 2 && members.count { it.value } < 2 -> ConnectionDisplayState.Waiting
        else -> ConnectionDisplayState.Connected
    }
