package com.example.couplecanvas.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.MemoryItem
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.max

object ShareCardGenerator {
    const val CARD_WIDTH = 1080
    const val CARD_HEIGHT = 1920
    const val COLLAGE_SIZE = 1080

    private const val WARM_CANVAS = 0xFFF6F4EF.toInt()
    private const val WARM_SURFACE = Color.WHITE
    private const val WARM_BLACK = 0xFF2A2926.toInt()
    private const val WARM_GRAY = 0xFF706B63.toInt()
    private const val SUNSHINE_YELLOW = 0xFFFFD45D.toInt()
    private const val SUNSHINE_YELLOW_DEEP = 0xFFE3AA00.toInt()
    private const val SOFT_PINK = 0xFFFFA7B8.toInt()
    private const val RAUSCH = SUNSHINE_YELLOW_DEEP
    private const val SAND = 0xFFE3E5EA.toInt()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun createDatePlanCardUri(context: Context, roomTitle: String, plan: DatePlan): Uri {
        val bitmap = createBaseCard(roomTitle = roomTitle, eyebrow = plan.tone.toneLabelForShare())
        val canvas = Canvas(bitmap)
        var y = 500f
        y = drawTitle(canvas, plan.title, 92f, y, 880f, 74f)
        y += 32f
        y = drawMultiline(canvas, plan.description, 92f, y, 880, 38f, WARM_GRAY)
        y += 36f
        drawPill(canvas, "분위기 ${plan.vibe.vibeLabelForShare()}", 92f, y)
        drawPill(canvas, plan.estimatedTime ?: "시간 미정", 360f, y)
        drawPill(canvas, plan.estimatedBudget ?: "예산 미정", 600f, y)
        y += 140f
        drawSectionLabel(canvas, "오늘의 플랜", 92f, y)
        y += 68f
        plan.steps.take(4).forEachIndexed { index, step ->
            val top = y
            drawCircleNumber(canvas, index + 1, 116f, top + 32f)
            y = drawMultiline(canvas, step, 168f, top, 800, 36f, WARM_BLACK)
            y += 28f
        }
        val status = if (plan.scheduledAt != null) {
            "예정일 ${plan.scheduledAt.toShareDate()}"
        } else {
            "둘만의 일정으로 저장해요"
        }
        drawFooter(canvas, status)
        return saveBitmap(context, bitmap, "date-plan-${plan.planId.ifBlank { UUID.randomUUID().toString() }}.png")
    }

    fun createMemoryCardUri(context: Context, roomTitle: String, memory: MemoryItem): Uri {
        val bitmap = createBaseCard(roomTitle = roomTitle, eyebrow = "Memory Scrapbook")
        val canvas = Canvas(bitmap)
        var y = 500f
        val photos = memory.imageUrls.take(4).mapNotNull { loadBitmap(context, it) }
        if (photos.isNotEmpty()) {
            drawPhotoCollage(canvas, photos, RectF(92f, y, 988f, y + 620f))
            y += 700f
        }
        y = drawTitle(canvas, memory.title, 92f, y, 880f, 72f)
        y += 24f
        memory.note?.takeIf { it.isNotBlank() }?.let {
            y = drawMultiline(canvas, it, 92f, y, 880, 40f, WARM_GRAY)
            y += 24f
        }
        drawPill(canvas, memory.date.toShareDate(), 92f, y)
        drawPill(canvas, "사진 ${memory.imageUrls.size}장", 330f, y)
        drawFooter(canvas, "우리의 추억을 Couple Canvas에 저장했어요")
        return saveBitmap(context, bitmap, "memory-${memory.memoryId.ifBlank { UUID.randomUUID().toString() }}.png")
    }

    fun createMemoryCollageUri(context: Context, memory: MemoryItem): Uri {
        val photos = memory.imageUrls.take(4).mapNotNull { loadBitmap(context, it) }
        require(photos.isNotEmpty()) { "콜라주로 만들 사진이 없어요" }

        val bitmap = Bitmap.createBitmap(COLLAGE_SIZE, COLLAGE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(WARM_CANVAS)
        drawPhotoCollage(canvas, photos, RectF(48f, 48f, 1032f, 1032f))
        return saveBitmap(context, bitmap, "memory-collage-${memory.memoryId.ifBlank { UUID.randomUUID().toString() }}.png")
    }

    fun shareImage(context: Context, uri: Uri, chooserTitle: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun createBaseCard(roomTitle: String, eyebrow: String): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(WARM_CANVAS)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WARM_SURFACE
            setShadowLayer(22f, 0f, 12f, 0x22000000)
        }
        canvas.drawRoundRect(RectF(56f, 92f, 1024f, 1828f), 56f, 56f, cardPaint)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUNSHINE_YELLOW }
        canvas.drawRoundRect(RectF(56f, 92f, 1024f, 430f), 56f, 56f, headerPaint)
        canvas.drawRect(56f, 360f, 1024f, 430f, headerPaint)
        drawText(canvas, "Couple Canvas", 92f, 178f, 36f, WARM_BLACK, true)
        drawText(canvas, roomTitle.ifBlank { "둘만의 그림방" }, 92f, 235f, 54f, WARM_BLACK, true)
        drawText(canvas, eyebrow, 92f, 306f, 34f, WARM_GRAY, false)
        drawBrandMark(canvas)
        return bitmap
    }

    private fun drawFooter(canvas: Canvas, text: String) {
        val y = 1668f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SAND }
        canvas.drawRoundRect(RectF(92f, y, 988f, y + 96f), 48f, 48f, paint)
        drawText(canvas, text, 132f, y + 61f, 32f, WARM_BLACK, true)
        drawText(canvas, "made with Couple Canvas", 92f, 1810f, 28f, WARM_GRAY, false)
    }

    private fun drawPhotoCollage(canvas: Canvas, photos: List<Bitmap>, bounds: RectF) {
        val gap = 14f
        when (photos.size) {
            1 -> drawCroppedBitmap(canvas, photos[0], bounds, 36f)
            2 -> {
                val half = (bounds.width() - gap) / 2f
                drawCroppedBitmap(canvas, photos[0], RectF(bounds.left, bounds.top, bounds.left + half, bounds.bottom), 32f)
                drawCroppedBitmap(canvas, photos[1], RectF(bounds.left + half + gap, bounds.top, bounds.right, bounds.bottom), 32f)
            }
            3 -> {
                val leftWidth = (bounds.width() - gap) * 0.58f
                drawCroppedBitmap(canvas, photos[0], RectF(bounds.left, bounds.top, bounds.left + leftWidth, bounds.bottom), 32f)
                val rightLeft = bounds.left + leftWidth + gap
                val halfHeight = (bounds.height() - gap) / 2f
                drawCroppedBitmap(canvas, photos[1], RectF(rightLeft, bounds.top, bounds.right, bounds.top + halfHeight), 32f)
                drawCroppedBitmap(canvas, photos[2], RectF(rightLeft, bounds.top + halfHeight + gap, bounds.right, bounds.bottom), 32f)
            }
            else -> {
                val cellWidth = (bounds.width() - gap) / 2f
                val cellHeight = (bounds.height() - gap) / 2f
                photos.take(4).forEachIndexed { index, photo ->
                    val col = index % 2
                    val row = index / 2
                    val left = bounds.left + col * (cellWidth + gap)
                    val top = bounds.top + row * (cellHeight + gap)
                    drawCroppedBitmap(canvas, photo, RectF(left, top, left + cellWidth, top + cellHeight), 28f)
                }
            }
        }
    }

    private fun drawCroppedBitmap(canvas: Canvas, bitmap: Bitmap, bounds: RectF, radius: Float) {
        val path = android.graphics.Path().apply { addRoundRect(bounds, radius, radius, android.graphics.Path.Direction.CW) }
        val save = canvas.save()
        canvas.clipPath(path)
        val scale = max(bounds.width() / bitmap.width, bounds.height() / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val left = bounds.left + (bounds.width() - scaledWidth) / 2f
        val top = bounds.top + (bounds.height() - scaledHeight) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledWidth, top + scaledHeight), Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restoreToCount(save)
    }

    private fun drawBrandMark(canvas: Canvas) {
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WARM_SURFACE }
        val tile = RectF(800f, 172f, 970f, 342f)
        canvas.drawRoundRect(tile, 42f, 42f, tilePaint)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFD45D }
        val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SUNSHINE_YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 26f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val pinkPaint = Paint(yellowPaint).apply { color = SOFT_PINK }
        val highlightPaint = Paint(yellowPaint).apply {
            color = Color.WHITE
            strokeWidth = 5f
        }
        val shadow = android.graphics.Path().apply {
            moveTo(838f, 308f)
            cubicTo(862f, 328f, 922f, 328f, 946f, 304f)
            cubicTo(918f, 316f, 872f, 318f, 838f, 308f)
        }
        canvas.drawPath(shadow, shadowPaint)
        val yellow = android.graphics.Path().apply {
            moveTo(874f, 296f)
            cubicTo(824f, 258f, 822f, 204f, 858f, 190f)
            cubicTo(890f, 178f, 900f, 214f, 888f, 248f)
        }
        val pink = android.graphics.Path().apply {
            moveTo(898f, 246f)
            cubicTo(910f, 198f, 956f, 196f, 966f, 232f)
            cubicTo(980f, 280f, 922f, 308f, 872f, 312f)
        }
        canvas.drawPath(yellow, yellowPaint)
        canvas.drawPath(pink, pinkPaint)
        canvas.drawPath(android.graphics.Path().apply {
            moveTo(852f, 214f)
            cubicTo(862f, 202f, 876f, 200f, 884f, 206f)
        }, highlightPaint)
        canvas.drawPath(android.graphics.Path().apply {
            moveTo(940f, 214f)
            cubicTo(952f, 214f, 960f, 222f, 962f, 232f)
        }, highlightPaint)
        val sparkle = android.graphics.Path().apply {
            moveTo(940f, 286f)
            lineTo(948f, 304f)
            lineTo(966f, 312f)
            lineTo(948f, 320f)
            lineTo(940f, 338f)
            lineTo(932f, 320f)
            lineTo(914f, 312f)
            lineTo(932f, 304f)
            close()
        }
        canvas.drawPath(sparkle, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUNSHINE_YELLOW_DEEP })
        canvas.drawCircle(918f, 332f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SOFT_PINK })
    }

    private fun drawPill(canvas: Canvas, text: String, x: Float, y: Float) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WARM_BLACK
            textSize = 30f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val width = textPaint.measureText(text) + 48f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SAND }
        canvas.drawRoundRect(RectF(x, y, x + width, y + 66f), 33f, 33f, bg)
        canvas.drawText(text, x + 24f, y + 43f, textPaint)
    }

    private fun drawSectionLabel(canvas: Canvas, text: String, x: Float, y: Float) {
        drawText(canvas, text, x, y, 34f, RAUSCH, true)
    }

    private fun drawCircleNumber(canvas: Canvas, number: Int, x: Float, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RAUSCH }
        canvas.drawCircle(x, y, 34f, paint)
        drawText(canvas, number.toString(), x, y + 12f, 30f, Color.WHITE, true, Paint.Align.CENTER)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        align: Paint.Align = Paint.Align.LEFT,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        canvas.drawText(text, x, y, paint)
    }

    private fun drawTitle(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        preferredSize: Float,
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WARM_BLACK
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        var size = preferredSize
        while (size >= 56f) {
            paint.textSize = size
            if (paint.measureText(text) <= width) {
                return drawTitleLines(canvas, listOf(text), paint, x, y)
            }
            size -= 2f
        }

        paint.textSize = size
        val lines = wrapTitleByWords(text, paint, width).take(2).toMutableList()
        if (lines.size == 2) {
            lines[1] = ellipsizeToWidth(lines[1], paint, width)
        }
        return drawTitleLines(canvas, lines, paint, x, y)
    }

    private fun drawTitleLines(
        canvas: Canvas,
        lines: List<String>,
        paint: Paint,
        x: Float,
        y: Float,
    ): Float {
        val metrics = paint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent) * 1.08f
        var baseline = y - metrics.ascent
        lines.forEach { line ->
            canvas.drawText(line, x, baseline, paint)
            baseline += lineHeight
        }
        return y + lineHeight * lines.size
    }

    private fun wrapTitleByWords(text: String, paint: Paint, width: Float): List<String> {
        val words = text.split(" ").filter { it.isNotBlank() }
        if (words.size <= 1) return listOf(ellipsizeToWidth(text, paint, width))
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= width || current.isBlank()) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun ellipsizeToWidth(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        val suffix = "..."
        var value = text
        while (value.isNotEmpty() && paint.measureText(value + suffix) > width) {
            value = value.dropLast(1)
        }
        return value.trimEnd() + suffix
    }

    private fun drawMultiline(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        size: Float,
        color: Int,
        align: Paint.Align = Paint.Align.LEFT,
        bold: Boolean = false,
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            textAlign = align
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(size * 0.18f, 1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String): Uri {
        val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun loadBitmap(context: Context, source: String): Bitmap? =
        runCatching {
            when {
                source.startsWith("content://") || source.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                source.startsWith("/") -> {
                    File(source).inputStream().use { input -> BitmapFactory.decodeStream(input) }
                }
                else -> {
                    val connection = URL(source).openConnection().apply {
                        connectTimeout = 3_000
                        readTimeout = 3_000
                    }
                    connection.getInputStream().use { input -> BitmapFactory.decodeStream(input) }
                }
            }
        }.getOrNull()

    private fun Long?.toShareDate(): String =
        this?.takeIf { it > 0L }?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        } ?: "날짜 미정"

    private fun String.toneLabelForShare(): String = when (this) {
        "safe" -> "편안한 데이트"
        "playful" -> "재밌는 데이트"
        "bold" -> "새로운 도전"
        else -> this
    }

    private fun String.vibeLabelForShare(): String = when (this) {
        "Cozy" -> "포근"
        "Adventure" -> "모험"
        "Cute" -> "귀여움"
        "Chill" -> "느긋"
        "Foodie" -> "푸드"
        "Creative" -> "창작"
        "Movie Night" -> "영화"
        "Walk & Talk" -> "산책"
        "Long Distance Call" -> "원거리"
        else -> this
    }
}
