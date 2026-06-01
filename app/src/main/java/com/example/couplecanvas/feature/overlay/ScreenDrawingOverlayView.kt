package com.example.couplecanvas.feature.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.couplecanvas.data.model.Stroke

class ScreenDrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onStartStroke: ((Float, Float) -> Unit)? = null
    var onMoveStroke: ((Float, Float) -> Unit)? = null
    var onEndStroke: (() -> Unit)? = null

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(18, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val messageBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 29, 29, 31)
        style = Paint.Style.FILL
    }
    private val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = dp(13).toFloat()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val messageRect = RectF()
    private var strokes: List<Stroke> = emptyList()
    private var message: String? = null
    private var drawingEnabled = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    fun setOverlayContent(
        strokes: List<Stroke>,
        message: String?,
        drawingEnabled: Boolean,
    ) {
        val sorted = strokes.sortedBy { it.createdAt }
        if (this.strokes == sorted && this.message == message && this.drawingEnabled == drawingEnabled) return
        this.strokes = sorted
        this.message = message
        this.drawingEnabled = drawingEnabled
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        OverlayStrokeRenderer.drawStrokes(canvas, strokes, width.toFloat(), height.toFloat(), now)
        message?.takeIf { it.isNotBlank() }?.let { drawMessage(canvas, it) }
        if (strokes.any { it.expiresAt > 0L && !it.isExpired(now) }) {
            postInvalidateDelayed(48L)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled || width <= 0 || height <= 0) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                val point = normalized(event.x, event.y)
                onStartStroke?.invoke(point.first, point.second)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId).takeIf { it >= 0 } ?: 0
                for (historyIndex in 0 until event.historySize) {
                    val point = normalized(
                        event.getHistoricalX(pointerIndex, historyIndex),
                        event.getHistoricalY(pointerIndex, historyIndex),
                    )
                    onMoveStroke?.invoke(point.first, point.second)
                }
                val point = normalized(event.getX(pointerIndex), event.getY(pointerIndex))
                onMoveStroke?.invoke(point.first, point.second)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val pointerIndex = event.findPointerIndex(activePointerId).takeIf { it >= 0 } ?: 0
                val point = normalized(event.getX(pointerIndex), event.getY(pointerIndex))
                onMoveStroke?.invoke(point.first, point.second)
                onEndStroke?.invoke()
                activePointerId = MotionEvent.INVALID_POINTER_ID
                return true
            }
        }
        return true
    }

    private fun normalized(x: Float, y: Float): Pair<Float, Float> =
        (x / width.toFloat()).coerceIn(0f, 1f) to (y / height.toFloat()).coerceIn(0f, 1f)

    private fun drawMessage(canvas: Canvas, text: String) {
        val horizontalPadding = dp(18).toFloat()
        val verticalPadding = dp(11).toFloat()
        val textWidth = messagePaint.measureText(text)
        val centerX = width / 2f
        val centerY = height / 2f
        messageRect.set(
            centerX - textWidth / 2f - horizontalPadding,
            centerY - dp(22),
            centerX + textWidth / 2f + horizontalPadding,
            centerY + dp(22),
        )
        canvas.drawRoundRect(messageRect, dp(22).toFloat(), dp(22).toFloat(), messageBackgroundPaint)
        val baseline = centerY - (messagePaint.descent() + messagePaint.ascent()) / 2f
        canvas.drawText(text, centerX, baseline, messagePaint)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
