package com.example.couplecanvas.util

import kotlin.math.ceil
import kotlin.math.sqrt

object StrokeInputInterpolator {
    private const val MAX_NORMALIZED_STEP = 0.0065f

    fun interpolatedPoints(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
    ): List<Pair<Float, Float>> {
        val startX = fromX.safeNormalized()
        val startY = fromY.safeNormalized()
        val endX = toX.safeNormalized()
        val endY = toY.safeNormalized()
        val dx = endX - startX
        val dy = endY - startY
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= 0f) return emptyList()

        val steps = ceil(distance / MAX_NORMALIZED_STEP).toInt().coerceAtLeast(1)
        return (1..steps).map { step ->
            val progress = step.toFloat() / steps.toFloat()
            (startX + dx * progress).safeNormalized() to (startY + dy * progress).safeNormalized()
        }
    }

    private fun Float.safeNormalized(): Float =
        if (isNaN() || isInfinite()) 0f else coerceIn(0f, 1f)
}
