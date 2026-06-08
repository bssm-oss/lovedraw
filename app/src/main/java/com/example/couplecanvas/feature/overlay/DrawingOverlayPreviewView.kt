package com.example.couplecanvas.feature.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.Stroke
import kotlin.math.roundToInt

class DrawingOverlayPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 255, 122, 154)
        style = Paint.Style.STROKE
        strokeWidth = dp(1).toFloat()
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(126, 104, 111)
        textAlign = Paint.Align.CENTER
        textSize = dp(12).toFloat()
    }
    private val rect = RectF()
    private val clipPath = Path()
    private var strokes: List<Stroke> = emptyList()
    private var privacyMode: Boolean = false

    fun setPreview(strokes: List<Stroke>, privacyMode: Boolean) {
        val next = strokes.sortedBy { it.createdAt }
        if (this.strokes == next && this.privacyMode == privacyMode) return
        this.strokes = next
        this.privacyMode = privacyMode
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dp(238)
        val desiredHeight = dp(112)
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        val radius = dp(14).toFloat()
        canvas.drawRoundRect(rect, radius, radius, backgroundPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        val text = when {
            privacyMode -> "프라이버시 모드"
            strokes.isEmpty() -> "여기에 낙서가 보여요"
            else -> null
        }
        if (text != null) {
            val y = height / 2f - (placeholderPaint.descent() + placeholderPaint.ascent()) / 2f
            canvas.drawText(text, width / 2f, y, placeholderPaint)
            return
        }

        clipPath.reset()
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        OverlayStrokeRenderer.drawStrokes(canvas, strokes, width.toFloat(), height.toFloat())
        canvas.restore()
        if (strokes.any { it.expiresAt > 0L && !it.isExpired() }) {
            postInvalidateDelayed(48L)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

object OverlayStrokeRenderer {
    private const val FALLBACK_COLOR = "#222222"

    fun drawStrokes(canvas: Canvas, strokes: List<Stroke>, width: Float, height: Float, nowMillis: Long = System.currentTimeMillis()) {
        if (width <= 0f || height <= 0f || strokes.isEmpty()) return
        val layer = canvas.saveLayer(0f, 0f, width, height, null)
        strokes.sortedBy { it.createdAt }.forEach { stroke ->
            if (stroke.isExpired(nowMillis)) return@forEach
            val points = stroke.sortedPoints()
            if (points.isEmpty()) return@forEach
            val alpha = stroke.alpha(nowMillis)
            val haloPaint = if (stroke.eraser) null else strokeHaloPaint(stroke, alpha)
            val markerUnderlayPaint = if (stroke.eraser) null else markerUnderlayPaint(stroke, alpha)
            val paint = strokePaint(stroke, alpha)
            if (points.size == 1) {
                val point = points.first()
                haloPaint?.let {
                    it.style = Paint.Style.FILL
                    canvas.drawCircle(point.safeX() * width, point.safeY() * height, it.strokeWidth / 2f, it)
                }
                markerUnderlayPaint?.let {
                    it.style = Paint.Style.FILL
                    canvas.drawCircle(point.safeX() * width, point.safeY() * height, it.strokeWidth / 2f, it)
                }
                paint.style = Paint.Style.FILL
                canvas.drawCircle(point.safeX() * width, point.safeY() * height, paint.strokeWidth / 2f, paint)
                return@forEach
            }
            val path = points.toPath(width, height)
            haloPaint?.let { canvas.drawPath(path, it) }
            markerUnderlayPaint?.let { canvas.drawPath(path, it) }
            canvas.drawPath(path, paint)
        }
        canvas.restoreToCount(layer)
    }

    private fun strokeHaloPaint(stroke: Stroke, alpha: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((72 * alpha.coerceIn(0f, 1f)).roundToInt(), 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = stroke.width.coerceAtLeast(1f) + 12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private fun markerUnderlayPaint(stroke: Stroke, alpha: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(stroke.color).withAlpha(alpha * 0.42f)
            style = Paint.Style.STROKE
            strokeWidth = stroke.width.coerceAtLeast(1f) * 1.55f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private fun strokePaint(stroke: Stroke, alpha: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (stroke.eraser) Color.TRANSPARENT else parseColor(stroke.color).withAlpha(alpha)
            style = Paint.Style.STROKE
            strokeWidth = stroke.width.coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            xfermode = if (stroke.eraser) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
        }

    private fun List<DrawingPoint>.toPath(width: Float, height: Float): Path {
        val path = Path()
        val first = first()
        path.moveTo(first.safeX() * width, first.safeY() * height)
        if (size == 2) {
            val last = last()
            path.lineTo(last.safeX() * width, last.safeY() * height)
            return path
        }
        for (index in 1 until size - 1) {
            val current = this[index]
            val next = this[index + 1]
            val currentX = current.safeX() * width
            val currentY = current.safeY() * height
            val nextX = next.safeX() * width
            val nextY = next.safeY() * height
            path.quadTo(currentX, currentY, (currentX + nextX) / 2f, (currentY + nextY) / 2f)
        }
        val last = last()
        path.lineTo(last.safeX() * width, last.safeY() * height)
        return path
    }

    private fun DrawingPoint.safeX(): Float = x.coerceIn(0f, 1f)

    private fun DrawingPoint.safeY(): Float = y.coerceIn(0f, 1f)

    private fun parseColor(hex: String): Int =
        runCatching { Color.parseColor(hex) }.getOrElse { Color.parseColor(FALLBACK_COLOR) }

    private fun Int.withAlpha(alpha: Float): Int =
        Color.argb(
            (Color.alpha(this) * alpha.coerceIn(0f, 1f)).roundToInt(),
            Color.red(this),
            Color.green(this),
            Color.blue(this),
        )
}
