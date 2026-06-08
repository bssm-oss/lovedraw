package com.example.couplecanvas

import com.example.couplecanvas.data.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeExpiryTest {
    @Test
    fun persistentStrokeNeverExpires() {
        val stroke = Stroke(expiresAt = 0L)

        assertFalse(stroke.isExpired(nowMillis = 10_000L))
        assertEquals(1f, stroke.alpha(nowMillis = 10_000L), 0.001f)
    }

    @Test
    fun laserStrokeExpiresAndFades() {
        val stroke = Stroke(expiresAt = 10_000L)

        assertEquals(1f, stroke.alpha(nowMillis = 10_000L - Stroke.LASER_FADE_MS - 1L), 0.001f)
        assertFalse(stroke.isExpired(nowMillis = 9_500L))
        assertTrue(stroke.alpha(nowMillis = 9_500L) in 0f..1f)
        assertTrue(stroke.isExpired(nowMillis = 10_000L))
        assertEquals(0f, stroke.alpha(nowMillis = 10_000L), 0.001f)
    }

    @Test
    fun laserStrokeFadesSmoothlyDuringFadeWindow() {
        val stroke = Stroke(expiresAt = 10_000L)
        val halfway = 10_000L - Stroke.LASER_FADE_MS / 2

        assertEquals(0.5f, stroke.alpha(nowMillis = halfway), 0.01f)
    }

    @Test
    fun laserStrokeUsesSoftFadeCurve() {
        val stroke = Stroke(expiresAt = 10_000L)
        val quarterRemaining = 10_000L - Stroke.LASER_FADE_MS / 4
        val threeQuarterRemaining = 10_000L - Stroke.LASER_FADE_MS * 3 / 4

        assertTrue(stroke.alpha(nowMillis = threeQuarterRemaining) > 0.75f)
        assertTrue(stroke.alpha(nowMillis = quarterRemaining) < 0.25f)
    }
}
