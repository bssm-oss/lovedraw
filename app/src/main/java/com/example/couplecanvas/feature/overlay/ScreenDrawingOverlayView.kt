package com.example.couplecanvas.feature.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.couplecanvas.data.model.BrushState
import com.example.couplecanvas.data.model.Stroke

class ScreenDrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onStartStroke: ((Float, Float) -> Unit)? = null
    var onMoveStroke: ((Float, Float) -> Unit)? = null
    var onEndStroke: (() -> Unit)? = null
    var onBrushColorSelected: ((String) -> Unit)? = null
    var onEraserSelected: (() -> Unit)? = null
    var onCloseDrawing: (() -> Unit)? = null

    private val toolbarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(178, 44, 44, 48)
        style = Paint.Style.FILL
    }
    private val toolbarBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(54, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(1).toFloat()
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = dp(2).toFloat()
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(2).toFloat()
    }
    private val swatchBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(1).toFloat()
    }
    private val eraserFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 245, 245)
        style = Paint.Style.FILL
    }
    private val toolbarRect = RectF()
    private val closeBounds = RectF()
    private val eraserBounds = RectF()
    private val eraserPath = Path()
    private val palette = listOf(
        "#E5484D" to Color.rgb(229, 72, 77),
        "#1E63D6" to Color.rgb(30, 99, 214),
        "#222222" to Color.rgb(34, 34, 34),
    )
    private val swatchBounds = mutableListOf<RectF>()
    private var pendingToolbarAction: ToolbarAction? = null
    private var brush: BrushState = BrushState(color = "#E5484D", eraser = false)
    private var strokes: List<Stroke> = emptyList()
    private var drawingEnabled = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    init {
        setWillNotDraw(false)
    }

    fun setOverlayContent(
        strokes: List<Stroke>,
        brush: BrushState,
        drawingEnabled: Boolean,
    ) {
        val sorted = strokes.sortedBy { it.createdAt }
        if (this.strokes == sorted && this.brush == brush && this.drawingEnabled == drawingEnabled) return
        this.strokes = sorted
        this.brush = brush
        this.drawingEnabled = drawingEnabled
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        OverlayStrokeRenderer.drawStrokes(canvas, strokes, width.toFloat(), height.toFloat(), now)
        if (drawingEnabled) drawToolbar(canvas)
        if (strokes.any { it.expiresAt > 0L && !it.isExpired(now) }) {
            postInvalidateDelayed(48L)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled || width <= 0 || height <= 0) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                toolbarActionAt(event.x, event.y)?.let { action ->
                    pendingToolbarAction = action
                    return true
                }
                activePointerId = event.getPointerId(0)
                val point = normalized(event.x, event.y)
                onStartStroke?.invoke(point.first, point.second)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pendingToolbarAction != null) return true
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
                pendingToolbarAction?.let { action ->
                    if (event.actionMasked == MotionEvent.ACTION_UP && toolbarActionAt(event.x, event.y) == action) {
                        applyToolbarAction(action)
                    }
                    pendingToolbarAction = null
                    return true
                }
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

    private fun drawToolbar(canvas: Canvas) {
        updateToolbarGeometry()
        val radius = dp(28).toFloat()
        canvas.drawRoundRect(toolbarRect, radius, radius, toolbarPaint)
        canvas.drawRoundRect(toolbarRect, radius, radius, toolbarBorderPaint)
        drawCloseIcon(canvas)
        palette.forEachIndexed { index, (_, color) ->
            val bounds = swatchBounds[index]
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2f, fillPaint)
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2f, swatchBorderPaint)
            if (!brush.eraser && brush.color.equals(palette[index].first, ignoreCase = true)) {
                canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2f + dp(5), selectedPaint)
            }
        }
        drawEraserIcon(canvas)
    }

    private fun drawCloseIcon(canvas: Canvas) {
        val inset = dp(11).toFloat()
        canvas.drawLine(closeBounds.left + inset, closeBounds.top + inset, closeBounds.right - inset, closeBounds.bottom - inset, iconPaint)
        canvas.drawLine(closeBounds.right - inset, closeBounds.top + inset, closeBounds.left + inset, closeBounds.bottom - inset, iconPaint)
    }

    private fun drawEraserIcon(canvas: Canvas) {
        val selected = brush.eraser
        if (selected) {
            canvas.drawCircle(eraserBounds.centerX(), eraserBounds.centerY(), eraserBounds.width() / 2f + dp(5), selectedPaint)
        }
        val cx = eraserBounds.centerX()
        val cy = eraserBounds.centerY()
        val halfW = dp(11).toFloat()
        val halfH = dp(7).toFloat()
        eraserPath.reset()
        eraserPath.moveTo(cx - halfW, cy - halfH + dp(3))
        eraserPath.lineTo(cx - dp(3), cy - halfH - dp(5))
        eraserPath.lineTo(cx + halfW, cy + halfH - dp(3))
        eraserPath.lineTo(cx + dp(3), cy + halfH + dp(5))
        eraserPath.close()
        canvas.drawPath(eraserPath, eraserFillPaint)
        canvas.drawPath(eraserPath, iconPaint)
    }

    private fun updateToolbarGeometry() {
        val buttonSize = dp(40).toFloat()
        val gap = dp(14).toFloat()
        val horizontalPadding = dp(16).toFloat()
        val widthNeeded = buttonSize * 5 + gap * 4 + horizontalPadding * 2
        val toolbarWidth = widthNeeded.coerceAtMost(width.toFloat() - dp(28))
        val toolbarHeight = dp(58).toFloat()
        val centerX = width / 2f
        val bottomMargin = dp(24).toFloat()
        toolbarRect.set(
            centerX - toolbarWidth / 2f,
            height - bottomMargin - toolbarHeight,
            centerX + toolbarWidth / 2f,
            height - bottomMargin,
        )
        val y = toolbarRect.centerY()
        var x = toolbarRect.left + horizontalPadding + buttonSize / 2f
        closeBounds.setCentered(x, y, buttonSize)
        x += buttonSize + gap
        while (swatchBounds.size < palette.size) swatchBounds.add(RectF())
        palette.indices.forEach { index ->
            swatchBounds[index].setCentered(x, y, dp(28).toFloat())
            x += buttonSize + gap
        }
        eraserBounds.setCentered(x, y, buttonSize)
    }

    private fun toolbarActionAt(x: Float, y: Float): ToolbarAction? {
        updateToolbarGeometry()
        if (!toolbarRect.contains(x, y)) return null
        if (closeBounds.contains(x, y)) return ToolbarAction.Close
        swatchBounds.forEachIndexed { index, bounds ->
            if (bounds.insetContains(x, y, dp(8).toFloat())) return ToolbarAction.Color(palette[index].first)
        }
        if (eraserBounds.contains(x, y)) return ToolbarAction.Eraser
        return ToolbarAction.None
    }

    private fun applyToolbarAction(action: ToolbarAction) {
        when (action) {
            ToolbarAction.Close -> onCloseDrawing?.invoke()
            ToolbarAction.Eraser -> onEraserSelected?.invoke()
            ToolbarAction.None -> Unit
            is ToolbarAction.Color -> onBrushColorSelected?.invoke(action.hex)
        }
    }

    private fun RectF.setCentered(centerX: Float, centerY: Float, size: Float) {
        set(centerX - size / 2f, centerY - size / 2f, centerX + size / 2f, centerY + size / 2f)
    }

    private fun RectF.insetContains(x: Float, y: Float, inset: Float): Boolean =
        x >= left - inset && x <= right + inset && y >= top - inset && y <= bottom + inset

    private sealed interface ToolbarAction {
        data object Close : ToolbarAction
        data object Eraser : ToolbarAction
        data object None : ToolbarAction
        data class Color(val hex: String) : ToolbarAction
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
