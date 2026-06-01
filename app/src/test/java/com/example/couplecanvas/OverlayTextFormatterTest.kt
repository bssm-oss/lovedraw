package com.example.couplecanvas

import com.example.couplecanvas.data.model.WidgetSnapshot
import com.example.couplecanvas.feature.overlay.OverlayTextFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OverlayTextFormatterTest {
    @Test
    fun privacyModeHidesSensitiveOverlayText() {
        val text = OverlayTextFormatter.format(
            WidgetSnapshot(
                roomTitle = "비밀 데이트방",
                privacyMode = true,
                latestNoteText = "오늘 밤 9시에 편지 확인",
                nextDateText = "D-1 · 비밀 산책",
                dailySparkText = "우리만 아는 질문",
            ),
        )

        assertEquals("비밀 데이트방", text.title)
        assertEquals("새 기록이 있어요", text.body)
        assertEquals("앱에서 확인하기", text.helper)
        assertFalse(text.body.contains("오늘 밤"))
        assertFalse(text.body.contains("비밀 산책"))
        assertFalse(text.body.contains("우리만"))
    }

    @Test
    fun normalModePrioritizesRecentNoteForOverlay() {
        val text = OverlayTextFormatter.format(
            WidgetSnapshot(
                roomId = "room-1",
                roomTitle = "둘만의 그림방",
                latestNoteText = "새 노트 도착",
                nextDateText = "D-3 · 영화 보기",
            ),
        )

        assertEquals("둘만의 그림방", text.title)
        assertEquals("새 노트 도착", text.body)
        assertEquals("탭해서 방 열기", text.helper)
    }
}
