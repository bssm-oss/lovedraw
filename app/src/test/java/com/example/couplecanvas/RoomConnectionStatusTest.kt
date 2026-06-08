package com.example.couplecanvas

import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.util.ConnectionDisplayState
import com.example.couplecanvas.util.connectionDisplayState
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomConnectionStatusTest {
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
