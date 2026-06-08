package com.example.couplecanvas

import com.example.couplecanvas.util.PermissionOnboardingCopy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionOnboardingCopyTest {
    @Test
    fun overlayPermissionCopyExplainsWhyItIsRequired() {
        val combined = PermissionOnboardingCopy.HEADER_BODY + " " + PermissionOnboardingCopy.OVERLAY_BODY

        assertTrue(combined.contains("상대"))
        assertTrue(combined.contains("낙서"))
        assertTrue(combined.contains("화면 위"))
        assertTrue(combined.contains("권한"))
        assertTrue(combined.contains("처음 한 번"))
        assertTrue(combined.contains("다른 앱 위에 표시"))
    }

    @Test
    fun safetyCopyPromisesNoScreenCaptureOrSilentTracking() {
        val combined = PermissionOnboardingCopy.HEADER_BODY + " " + PermissionOnboardingCopy.OVERLAY_BODY + " " + PermissionOnboardingCopy.SAFETY_BODY

        assertTrue(combined.contains("화면 내용을 읽거나 저장하지 않아요"))
        assertTrue(combined.contains("화면 캡처 없음"))
        assertTrue(combined.contains("몰래 위치 추적 없음"))
        assertFalse(combined.contains("항상 위치"))
        assertFalse(combined.contains("백그라운드 위치"))
    }

    @Test
    fun locationPermissionCopyStaysOptionalAndUserInitiated() {
        assertTrue(PermissionOnboardingCopy.LOCATION_BODY.contains("직접 켤 때만"))
        assertTrue(PermissionOnboardingCopy.LOCATION_BODY.contains("기본값은 꺼짐"))
    }
}
