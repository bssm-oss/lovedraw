package com.example.couplecanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.feature.overlay.OverlayStrokeRenderer
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingCanvasRenderTest {
    @Test
    fun overlayRendererRendersSoftMarkerStroke() {
        val stroke = Stroke(
            strokeId = "test-stroke",
            ownerUid = "uid",
            color = Stroke.MARKER_RED,
            width = 18f,
            createdAt = 1L,
            points = mapOf(
                "00000" to DrawingPoint(0.18f, 0.18f, 1L),
                "00001" to DrawingPoint(0.50f, 0.50f, 2L),
                "00002" to DrawingPoint(0.82f, 0.82f, 3L),
            ),
        )
        val bitmap = markerBitmap(stroke)

        assertTrue(
            "Expected rendered overlay to contain a translucent pink marker stroke.",
            bitmap.containsPixel { red, green, blue, alpha ->
                alpha > 240 && red > 230 && green in 80..230 && blue in 95..230 && red > green && red > blue
            },
        )
    }

    @Test
    fun overlayRendererEraserClearsPreviousStrokeLayer() {
        val paintStroke = Stroke(
            strokeId = "paint-stroke",
            ownerUid = "uid",
            color = Stroke.MARKER_RED,
            width = 44f,
            createdAt = 1L,
            points = mapOf(
                "00000" to DrawingPoint(0.12f, 0.5f, 1L),
                "00001" to DrawingPoint(0.88f, 0.5f, 2L),
            ),
        )
        val eraserStroke = Stroke(
            strokeId = "eraser-stroke",
            ownerUid = "uid",
            width = 58f,
            eraser = true,
            createdAt = 2L,
            points = mapOf(
                "00000" to DrawingPoint(0.5f, 0.18f, 3L),
                "00001" to DrawingPoint(0.5f, 0.82f, 4L),
            ),
        )
        val bitmap = markerBitmap(paintStroke, eraserStroke)
        val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)

        assertTrue(
            "Expected eraser to reveal the white background at the crossing point.",
            Color.red(center) > 245 && Color.green(center) > 245 && Color.blue(center) > 245,
        )
    }

    private fun markerBitmap(vararg strokes: Stroke): Bitmap {
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        OverlayStrokeRenderer.drawStrokes(canvas, strokes.toList(), 320f, 320f, nowMillis = 4_000L)
        return bitmap
    }

    private fun Bitmap.containsPixel(predicate: (red: Int, green: Int, blue: Int, alpha: Int) -> Boolean): Boolean {
        for (x in 0 until width step 3) {
            for (y in 0 until height step 3) {
                val pixel = getPixel(x, y)
                if (predicate(Color.red(pixel), Color.green(pixel), Color.blue(pixel), Color.alpha(pixel))) {
                    return true
                }
            }
        }
        return false
    }
}
