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

        assertFalse(stroke.isExpired(nowMillis = 9_500L))
        assertTrue(stroke.alpha(nowMillis = 9_500L) in 0f..1f)
        assertTrue(stroke.isExpired(nowMillis = 10_000L))
        assertEquals(0f, stroke.alpha(nowMillis = 10_000L), 0.001f)
    }
}
