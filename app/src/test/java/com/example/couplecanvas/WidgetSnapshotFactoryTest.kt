package com.example.couplecanvas

import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.RoomHomeSummary
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.data.model.WidgetSnapshot
import com.example.couplecanvas.util.WidgetSnapshotFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetSnapshotFactoryTest {
    @Test
    fun homeSummaryShowsDaysTogetherAndRoomCounts() {
        val startedAt = LocalDate.now().minusDays(122)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val snapshot = WidgetSnapshotFactory.fromHomeSummary(
            RoomHomeSummary(
                room = Room(
                    roomId = "room-a",
                    title = "우리 방",
                    status = RoomStatus.Active.value,
                    startedAt = startedAt,
                ),
                noteCount = 3,
                unreadNoteCount = 1,
                datePlanCount = 2,
                memoryCount = 4,
                drawingSnapshotCount = 5,
                bucketItemCount = 6,
            ),
        )

        assertEquals("우리 함께한 지 123일", snapshot.daysTogetherText)
        assertEquals("새 노트 1개", snapshot.latestNoteText)
        assertEquals("데이트 플랜 2개", snapshot.nextDateText)
        assertEquals("추억 4개", snapshot.latestMemoryTitle)
        assertEquals("낙서 5개", snapshot.latestDrawingText)
        assertEquals("연결 · 추억 4 · 낙서 5 · 버킷 6", snapshot.statsText)
    }

    @Test
    fun homeSummaryPreservesCurrentRoomDrawingPreviewOnlyForSameRoom() {
        val current = WidgetSnapshot(
            roomId = "room-a",
            latestDrawingUrl = "https://example.test/drawing.png",
            latestDrawingLocalPath = "/private/latest.png",
            latestDrawingText = "최근 낙서가 있어요",
        )

        val sameRoom = WidgetSnapshotFactory.fromHomeSummary(
            RoomHomeSummary(room = Room(roomId = "room-a"), drawingSnapshotCount = 1),
            current,
        )
        val otherRoom = WidgetSnapshotFactory.fromHomeSummary(
            RoomHomeSummary(room = Room(roomId = "room-b"), drawingSnapshotCount = 1),
            current,
        )

        assertEquals("https://example.test/drawing.png", sameRoom.latestDrawingUrl)
        assertEquals("/private/latest.png", sameRoom.latestDrawingLocalPath)
        assertEquals("최근 낙서가 있어요", sameRoom.latestDrawingText)
        assertNull(otherRoom.latestDrawingUrl)
        assertNull(otherRoom.latestDrawingLocalPath)
        assertEquals("낙서 1개", otherRoom.latestDrawingText)
    }
}
