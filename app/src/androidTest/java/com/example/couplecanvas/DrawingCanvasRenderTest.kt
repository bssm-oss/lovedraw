package com.example.couplecanvas

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.DrawingUiState
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.presentation.screen.drawing.DrawingCanvas
import com.example.couplecanvas.presentation.theme.CoupleCanvasTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawingCanvasRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun drawingCanvasRendersPastelStroke() {
        val stroke = Stroke(
            strokeId = "test-stroke",
            ownerUid = "uid",
            color = "#FF7A9A",
            width = 18f,
            createdAt = 1L,
            points = mapOf(
                "00000" to DrawingPoint(0.18f, 0.18f, 1L),
                "00001" to DrawingPoint(0.82f, 0.82f, 2L),
            ),
        )

        composeRule.setContent {
            CoupleCanvasTheme {
                DrawingCanvas(
                    uiState = DrawingUiState(strokes = listOf(stroke), isLoading = false),
                    transformMode = false,
                    onStart = { _, _ -> },
                    onMove = { _, _ -> },
                    onEnd = {},
                    onTransform = { _, _, _ -> },
                    modifier = Modifier.size(260.dp),
                )
            }
        }

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        var foundStrokePixel = false
        for (x in 0 until pixels.width step 3) {
            for (y in 0 until pixels.height step 3) {
                val pixel = pixels[x, y]
                if (pixel.red > 0.85f && pixel.green in 0.25f..0.65f && pixel.blue in 0.35f..0.75f) {
                    foundStrokePixel = true
                    break
                }
            }
            if (foundStrokePixel) break
        }

        assertTrue("Expected the rendered canvas to contain the pastel stroke color.", foundStrokePixel)
    }

    @Test
    fun drawingCanvasRendersLocalPendingStrokeBeforeFirebaseEcho() {
        val pendingStroke = Stroke(
            strokeId = "pending-stroke",
            ownerUid = "uid",
            color = "#A9D6FF",
            width = 22f,
            createdAt = 2L,
            points = mapOf(
                "00000" to DrawingPoint(0.12f, 0.78f, 1L),
                "00001" to DrawingPoint(0.88f, 0.22f, 2L),
            ),
        )

        composeRule.setContent {
            CoupleCanvasTheme {
                DrawingCanvas(
                    uiState = DrawingUiState(localPendingStrokes = listOf(pendingStroke), isLoading = false),
                    transformMode = false,
                    onStart = { _, _ -> },
                    onMove = { _, _ -> },
                    onEnd = {},
                    onTransform = { _, _, _ -> },
                    modifier = Modifier.size(260.dp),
                )
            }
        }

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        var foundPendingPixel = false
        for (x in 0 until pixels.width step 3) {
            for (y in 0 until pixels.height step 3) {
                val pixel = pixels[x, y]
                if (pixel.red in 0.50f..0.80f && pixel.green > 0.72f && pixel.blue > 0.88f) {
                    foundPendingPixel = true
                    break
                }
            }
            if (foundPendingPixel) break
        }

        assertTrue("Expected local pending stroke to stay visible before Firebase echoes it.", foundPendingPixel)
    }

    @Test
    fun drawingCanvasEraserClearsPreviousStrokeLayer() {
        val paintStroke = Stroke(
            strokeId = "paint-stroke",
            ownerUid = "uid",
            color = "#FF7A9A",
            width = 42f,
            createdAt = 1L,
            points = mapOf(
                "00000" to DrawingPoint(0.12f, 0.5f, 1L),
                "00001" to DrawingPoint(0.88f, 0.5f, 2L),
            ),
        )
        val eraserStroke = Stroke(
            strokeId = "eraser-stroke",
            ownerUid = "uid",
            width = 56f,
            eraser = true,
            createdAt = 2L,
            points = mapOf(
                "00000" to DrawingPoint(0.5f, 0.18f, 3L),
                "00001" to DrawingPoint(0.5f, 0.82f, 4L),
            ),
        )

        composeRule.setContent {
            CoupleCanvasTheme {
                DrawingCanvas(
                    uiState = DrawingUiState(strokes = listOf(paintStroke, eraserStroke), isLoading = false),
                    transformMode = false,
                    onStart = { _, _ -> },
                    onMove = { _, _ -> },
                    onEnd = {},
                    onTransform = { _, _, _ -> },
                    modifier = Modifier.size(260.dp),
                )
            }
        }

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val centerPixel = pixels[pixels.width / 2, pixels.height / 2]

        assertTrue(
            "Expected eraser to clear the previous stroke instead of painting over it.",
            centerPixel.red > 0.94f && centerPixel.green > 0.94f && centerPixel.blue > 0.94f,
        )
    }
}
