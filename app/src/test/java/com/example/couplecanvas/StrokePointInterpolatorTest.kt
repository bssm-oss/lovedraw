package com.example.couplecanvas

import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.util.StrokePointInterpolator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePointInterpolatorTest {
    @Test
    fun normalizedPointsClampOutOfBoundsValues() {
        val points = StrokePointInterpolator.normalizedPoints(
            listOf(
                DrawingPoint(-0.5f, 1.7f, 1L),
                DrawingPoint(Float.NaN, Float.POSITIVE_INFINITY, 2L),
            ),
        )

        assertEquals(DrawingPoint(0f, 1f, 1L), points[0])
        assertEquals(DrawingPoint(0f, 0f, 2L), points[1])
    }

    @Test
    fun normalizedPointsDropTinyIntermediateJitterButKeepEndPoint() {
        val points = StrokePointInterpolator.normalizedPoints(
            listOf(
                DrawingPoint(0.1f, 0.1f, 1L),
                DrawingPoint(0.1001f, 0.1001f, 2L),
                DrawingPoint(0.1002f, 0.1002f, 3L),
                DrawingPoint(0.55f, 0.6f, 4L),
            ),
        )

        assertEquals(
            listOf(
                DrawingPoint(0.1f, 0.1f, 1L),
                DrawingPoint(0.55f, 0.6f, 4L),
            ),
            points,
        )
    }

    @Test
    fun cubicSegmentsFollowEveryNormalizedStrokeInterval() {
        val points = listOf(
            DrawingPoint(0.1f, 0.6f, 1L),
            DrawingPoint(0.35f, 0.2f, 2L),
            DrawingPoint(0.65f, 0.8f, 3L),
            DrawingPoint(0.9f, 0.35f, 4L),
        )

        val segments = StrokePointInterpolator.cubicSegments(points)

        assertEquals(3, segments.size)
        assertEquals(points.first(), segments.first().start)
        assertEquals(points.last(), segments.last().end)
    }

    @Test
    fun cubicSegmentControlsStayInsideNormalizedCanvas() {
        val segments = StrokePointInterpolator.cubicSegments(
            listOf(
                DrawingPoint(0f, 0f, 1L),
                DrawingPoint(0.2f, 1f, 2L),
                DrawingPoint(0.8f, 0f, 3L),
                DrawingPoint(1f, 1f, 4L),
            ),
        )

        val allControls = segments.flatMap { listOf(it.control1, it.control2) }

        assertTrue(allControls.isNotEmpty())
        assertTrue(allControls.all { it.x in 0f..1f && it.y in 0f..1f })
    }
}
