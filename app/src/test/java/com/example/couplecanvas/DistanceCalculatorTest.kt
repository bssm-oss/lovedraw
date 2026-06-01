package com.example.couplecanvas

import com.example.couplecanvas.data.model.LocationShareState
import com.example.couplecanvas.util.DistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {
    @Test
    fun distanceTextKeepsSharingOffByDefault() {
        val text = DistanceCalculator.distanceText(localUid = "me", shares = emptyList())

        assertEquals("위치 공유가 꺼져 있어요", text)
    }

    @Test
    fun distanceTextRequiresBothPeopleToOptIn() {
        val text = DistanceCalculator.distanceText(
            localUid = "me",
            shares = listOf(LocationShareState(uid = "me", enabled = true)),
        )

        assertEquals("상대방도 동의하면 거리만 표시돼요", text)
    }

    @Test
    fun distanceTextRequiresExplicitOneShotLocationsAfterMutualConsent() {
        val text = DistanceCalculator.distanceText(
            localUid = "me",
            shares = listOf(
                LocationShareState(uid = "me", enabled = true),
                LocationShareState(uid = "partner", enabled = true),
            ),
        )

        assertEquals("서로 동의했어요. 현재 위치를 1회 공유해주세요", text)
    }

    @Test
    fun distanceTextFormatsLastSharedDistanceOnlyAfterMutualConsentAndLocations() {
        val text = DistanceCalculator.distanceText(
            localUid = "me",
            shares = listOf(
                LocationShareState(uid = "me", enabled = true, latitude = 37.5665, longitude = 126.9780),
                LocationShareState(uid = "partner", enabled = true, latitude = 37.5700, longitude = 126.9820),
            ),
        )

        assertTrue(text.startsWith("마지막 공유 기준 "))
        assertTrue(text.endsWith(" 떨어져 있어요"))
    }
}
