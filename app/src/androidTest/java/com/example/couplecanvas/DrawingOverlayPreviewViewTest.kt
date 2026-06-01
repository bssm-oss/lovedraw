package com.example.couplecanvas

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.platform.app.InstrumentationRegistry
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.feature.overlay.DrawingOverlayPreviewView
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingOverlayPreviewViewTest {
    @Test
    fun overlayPreviewRendersFirebaseStrokePixels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = DrawingOverlayPreviewView(context)
        view.setPreview(
            listOf(
                Stroke(
                    strokeId = "overlay-stroke",
                    ownerUid = "uid",
                    color = "#FF7A9A",
                    width = 18f,
                    createdAt = 1L,
                    points = mapOf(
                        "00000" to DrawingPoint(0.12f, 0.5f, 1L),
                        "00001" to DrawingPoint(0.88f, 0.5f, 2L),
                    ),
                ),
            ),
            privacyMode = false,
        )
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(320, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(180, android.view.View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 320, 180)

        val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        var foundStrokePixel = false
        for (x in 0 until bitmap.width step 3) {
            for (y in 0 until bitmap.height step 3) {
                val pixel = bitmap.getPixel(x, y)
                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)
                if (red > 215 && green in 70..175 && blue in 90..200) {
                    foundStrokePixel = true
                    break
                }
            }
            if (foundStrokePixel) break
        }

        assertTrue("Expected overlay preview to contain the live drawing stroke color.", foundStrokePixel)
    }
}
