package com.example.couplecanvas

import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.util.ConnectionDisplayState
import com.example.couplecanvas.util.connectionDisplayState
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomConnectionStatusTest {
    @Test
    fun connectionStatesUseClearUserFacingCopy() {
        assertEquals("연결됨", ConnectionDisplayState.Connected.label)
        assertEquals("지금 그리면 상대 화면에 바로 보여요.", ConnectionDisplayState.Connected.description)
        assertEquals("상대 대기 중", ConnectionDisplayState.Waiting.label)
        assertEquals("초대 링크나 QR로 들어오면 바로 연결돼요.", ConnectionDisplayState.Waiting.description)
        assertEquals("재연결 중", ConnectionDisplayState.Reconnecting.label)
        assertEquals("인터넷 상태를 다시 확인하고 있어요.", ConnectionDisplayState.Reconnecting.description)
    }

    @Test
    fun accessibilityLabelCombinesStatusAndMeaning() {
        assertEquals(
            "재연결 중: 인터넷 상태를 다시 확인하고 있어요.",
            ConnectionDisplayState.Reconnecting.accessibilityLabel,
        )
    }

    @Test
    fun waitingRoomShowsPartnerWaitingWhenFirebaseIsConnected() {
        val room = Room(status = RoomStatus.Waiting.value, hostUid = "host", members = mapOf("host" to true))

        assertEquals(ConnectionDisplayState.Waiting, room.connectionDisplayState(firebaseConnected = true))
    }

    @Test
    fun joinedRoomShowsConnectedWhenGuestExists() {
        val room = Room(
            status = RoomStatus.Active.value,
            hostUid = "host",
            guestUid = "guest",
            members = mapOf("host" to true),
        )

        assertEquals(ConnectionDisplayState.Connected, room.connectionDisplayState(firebaseConnected = true))
    }

    @Test
    fun disconnectedRoomShowsReconnectingUnlessArchived() {
        val room = Room(
            status = RoomStatus.Active.value,
            hostUid = "host",
            guestUid = "guest",
            members = mapOf("host" to true, "guest" to true),
        )
        val archived = room.copy(status = RoomStatus.Closed.value)

        assertEquals(ConnectionDisplayState.Reconnecting, room.connectionDisplayState(firebaseConnected = false))
        assertEquals(ConnectionDisplayState.Archived, archived.connectionDisplayState(firebaseConnected = false))
    }
}
