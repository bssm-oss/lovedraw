package com.example.couplecanvas

import com.example.couplecanvas.util.OverlayQuickStartCopy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayQuickStartCopyTest {
    @Test
    fun quickStartCopyExplainsOverlayAndNotificationWithoutJargon() {
        val combined = listOf(
            OverlayQuickStartCopy.TITLE,
            OverlayQuickStartCopy.NOTIFICATION_BODY,
            OverlayQuickStartCopy.OVERLAY_BODY,
            OverlayQuickStartCopy.READY_BODY,
        ).joinToString(" ")

        assertTrue(combined.contains("화면 위"))
        assertTrue(combined.contains("상대"))
        assertTrue(combined.contains("낙서"))
        assertTrue(combined.contains("알림"))
        assertTrue(combined.contains("그리기 시작"))
        assertFalse(combined.contains("오버레이"))
    }
}
