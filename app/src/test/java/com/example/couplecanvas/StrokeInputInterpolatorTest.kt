package com.example.couplecanvas

import com.example.couplecanvas.util.StrokeInputInterpolator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeInputInterpolatorTest {
    @Test
    fun longMoveIsSplitIntoSmallerNormalizedSteps() {
        val points = StrokeInputInterpolator.interpolatedPoints(
            fromX = 0.1f,
            fromY = 0.1f,
            toX = 0.9f,
            toY = 0.1f,
        )

        assertTrue(points.size > 40)
        assertEquals(0.9f, points.last().first, 0.0001f)
        assertEquals(0.1f, points.last().second, 0.0001f)
    }

    @Test
    fun tinyMoveKeepsOnlyEndPoint() {
        val points = StrokeInputInterpolator.interpolatedPoints(
            fromX = 0.1f,
            fromY = 0.1f,
            toX = 0.102f,
            toY = 0.101f,
        )

        assertEquals(listOf(0.102f to 0.101f), points)
    }

    @Test
    fun invalidValuesAreClampedForOverlaySafety() {
        val points = StrokeInputInterpolator.interpolatedPoints(
            fromX = Float.NaN,
            fromY = -1f,
            toX = Float.POSITIVE_INFINITY,
            toY = 2f,
        )

        assertTrue(points.isNotEmpty())
        assertTrue(points.all { (x, y) -> x in 0f..1f && y in 0f..1f })
        assertEquals(0f, points.first().first, 0.0001f)
        assertEquals(1f, points.last().second, 0.0001f)
    }
}
