package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.DrawingPoint

data class StrokeCurveSegment(
    val start: DrawingPoint,
    val control1: DrawingPoint,
    val control2: DrawingPoint,
    val end: DrawingPoint,
)

object StrokePointInterpolator {
    private const val MIN_POINT_DISTANCE = 0.0012f
    private const val MIN_POINT_DISTANCE_SQUARED = MIN_POINT_DISTANCE * MIN_POINT_DISTANCE

    fun normalizedPoints(points: List<DrawingPoint>): List<DrawingPoint> {
        if (points.isEmpty()) return emptyList()
        val normalized = points.map { point ->
            point.copy(
                x = point.x.safeNormalized(),
                y = point.y.safeNormalized(),
            )
        }
        if (normalized.size <= 2) return normalized

        val filtered = mutableListOf(normalized.first())
        normalized.drop(1).dropLast(1).forEach { point ->
            if (filtered.last().distanceSquaredTo(point) >= MIN_POINT_DISTANCE_SQUARED) {
                filtered += point
            }
        }
        val last = normalized.last()
        if (filtered.size == 1 || filtered.last() != last) {
            filtered += last
        }
        return filtered
    }

    fun cubicSegments(points: List<DrawingPoint>): List<StrokeCurveSegment> {
        val normalized = normalizedPoints(points)
        if (normalized.size < 2) return emptyList()

        return (0 until normalized.lastIndex).map { index ->
            val previous = normalized.getOrElse(index - 1) { normalized[index] }
            val start = normalized[index]
            val end = normalized[index + 1]
            val next = normalized.getOrElse(index + 2) { end }

            StrokeCurveSegment(
                start = start,
                control1 = DrawingPoint(
                    x = (start.x + (end.x - previous.x) / 6f).safeNormalized(),
                    y = (start.y + (end.y - previous.y) / 6f).safeNormalized(),
                    t = start.t,
                ),
                control2 = DrawingPoint(
                    x = (end.x - (next.x - start.x) / 6f).safeNormalized(),
                    y = (end.y - (next.y - start.y) / 6f).safeNormalized(),
                    t = end.t,
                ),
                end = end,
            )
        }
    }

    private fun Float.safeNormalized(): Float =
        if (isNaN() || isInfinite()) 0f else coerceIn(0f, 1f)

    private fun DrawingPoint.distanceSquaredTo(other: DrawingPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }
}
