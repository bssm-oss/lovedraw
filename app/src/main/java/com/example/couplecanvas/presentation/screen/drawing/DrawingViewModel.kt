package com.example.couplecanvas.presentation.screen.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couplecanvas.data.model.BrushState
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.DrawingUiState
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.data.local.WidgetStateStore
import com.example.couplecanvas.data.model.CanvasEmoji
import com.example.couplecanvas.data.model.DrawingBackground
import com.example.couplecanvas.data.repository.AuthRepository
import com.example.couplecanvas.data.repository.DrawingRepository
import com.example.couplecanvas.data.repository.RoomRepository
import com.example.couplecanvas.feature.widgets.updateCoupleWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

class DrawingViewModel(
    private val roomId: String,
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val drawingRepository: DrawingRepository,
    private val widgetStateStore: WidgetStateStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrawingUiState(roomId = roomId))
    val uiState: StateFlow<DrawingUiState> = _uiState
    private var pointIndex = 0
    private var lastSentAt = 0L
    private var activeSendJob: Job? = null
    private val remoteLaserExpiries = mutableMapOf<String, RemoteLaserExpiry>()

    init {
        val uid = authRepository.currentUser?.uid
        viewModelScope.launch {
            roomRepository.observeFirebaseConnection().collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
        viewModelScope.launch {
            drawingRepository.observeFinishedStrokes(roomId).collect { strokes ->
                val now = System.currentTimeMillis()
                val remoteStrokes = strokes
                    .withReceiverLocalLaserExpiry(uid, now)
                    .filterNot { it.isExpired(now) }
                    .sortedBy { it.createdAt }
                val remoteIds = remoteStrokes.mapTo(mutableSetOf()) { it.strokeId }
                _uiState.value = _uiState.value.copy(
                    strokes = remoteStrokes,
                    localPendingStrokes = _uiState.value.localPendingStrokes
                        .filterNot { it.strokeId in remoteIds || it.isExpired(now) },
                    isLoading = false,
                    nowMillis = now,
                )
                uid?.let { cleanupExpiredOwnStrokes(strokes, it, now) }
            }
        }
        viewModelScope.launch {
            drawingRepository.observeActiveStrokes(roomId).collect { active ->
                val now = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    activeStrokes = active
                        .filter { it.ownerUid != uid }
                        .withReceiverLocalLaserExpiry(uid, now)
                        .filterNot { it.isExpired(now) },
                    nowMillis = now,
                )
            }
        }
        viewModelScope.launch {
            drawingRepository.observeSnapshots(roomId).collect { snapshots ->
                val sorted = snapshots.sortedByDescending { it.createdAt }
                _uiState.value = _uiState.value.copy(snapshots = sorted)
                sorted.firstOrNull()?.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    if (widgetStateStore.shouldUpdateRoom(roomId)) {
                        val localPreviewPath = runCatching {
                            widgetStateStore.cacheLatestDrawingPreviewFromUrl(roomId, imageUrl)
                        }.getOrNull()
                        widgetStateStore.updateLatestDrawing(roomId, imageUrl, localPreviewPath, afterSave = ::updateCoupleWidgets)
                    }
                }
            }
        }
        viewModelScope.launch {
            drawingRepository.observeEmojis(roomId).collect { emojis ->
                _uiState.value = _uiState.value.copy(emojis = emojis.sortedBy { it.createdAt })
            }
        }
        viewModelScope.launch {
            drawingRepository.observeBackground(roomId).collect { background ->
                _uiState.value = _uiState.value.copy(background = background)
            }
        }
        viewModelScope.launch {
            roomRepository.observeUsers(roomId).collect { users ->
                _uiState.value = _uiState.value.copy(partnerOnline = users.any { it.uid != uid && it.online })
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(250L)
                val now = System.currentTimeMillis()
                val state = _uiState.value
                val nextStrokes = state.strokes.filterNot { it.isExpired(now) }
                val nextPending = state.localPendingStrokes.filterNot { it.isExpired(now) }
                val nextActive = state.activeStrokes.filterNot { it.isExpired(now) }
                val nextLocalActive = state.localActiveStroke?.takeUnless { it.isExpired(now) }
                if (
                    nextStrokes.size != state.strokes.size ||
                    nextPending.size != state.localPendingStrokes.size ||
                    nextActive.size != state.activeStrokes.size ||
                    nextLocalActive?.strokeId != state.localActiveStroke?.strokeId ||
                    state.hasLaserStroke()
                ) {
                    _uiState.value = state.copy(
                        strokes = nextStrokes,
                        localPendingStrokes = nextPending,
                        activeStrokes = nextActive,
                        localActiveStroke = nextLocalActive,
                        nowMillis = now,
                    )
                }
                val localUid = authRepository.currentUser?.uid
                if (localUid != null && now % 4_000L < 260L) {
                    runCatching { drawingRepository.deleteExpiredStrokes(roomId, localUid, now) }
                }
            }
        }
        if (uid != null) viewModelScope.launch { roomRepository.updateOnlineStatus(roomId, uid, true) }
    }

    fun setBrushColor(color: String) {
        _uiState.value = _uiState.value.copy(brush = _uiState.value.brush.copy(color = color, eraser = false))
    }

    fun setBrushWidth(width: Float) {
        _uiState.value = _uiState.value.copy(brush = _uiState.value.brush.copy(width = width))
    }

    fun toggleEraser() {
        val brush = _uiState.value.brush
        _uiState.value = _uiState.value.copy(brush = brush.copy(eraser = !brush.eraser))
    }

    fun toggleLaser() {
        val brush = _uiState.value.brush
        _uiState.value = _uiState.value.copy(brush = brush.copy(laser = !brush.laser, eraser = false))
    }

    fun startStroke(x: Float, y: Float) {
        val uid = authRepository.currentUser?.uid ?: return
        pointIndex = 0
        val brush = _uiState.value.brush
        val now = System.currentTimeMillis()
        val point = DrawingPoint(x, y, now)
        val stroke = Stroke(
            strokeId = UUID.randomUUID().toString(),
            ownerUid = uid,
            color = brush.color,
            width = brush.width,
            eraser = false,
            createdAt = now,
            expiresAt = now + Stroke.LASER_TTL_MS,
            points = mapOf(pointKey() to point),
        )
        _uiState.value = _uiState.value.copy(localActiveStroke = stroke, nowMillis = now)
        sendActiveThrottled(force = true)
    }

    fun appendPoint(x: Float, y: Float) {
        val stroke = _uiState.value.localActiveStroke ?: return
        val now = System.currentTimeMillis()
        val updated = stroke.copy(
            points = stroke.points + (pointKey() to DrawingPoint(x, y, now)),
            expiresAt = now + Stroke.LASER_TTL_MS,
        )
        _uiState.value = _uiState.value.copy(localActiveStroke = updated, nowMillis = now)
        sendActiveThrottled()
    }

    fun finishStroke() {
        val uid = authRepository.currentUser?.uid ?: return
        val stroke = _uiState.value.localActiveStroke ?: return
        val now = System.currentTimeMillis()
        val finishedStroke = stroke.refreshedLaserExpiry()
        _uiState.value = _uiState.value.copy(
            localActiveStroke = null,
            localPendingStrokes = (_uiState.value.localPendingStrokes + finishedStroke).dedupPendingStrokes(),
            nowMillis = now,
        )
        activeSendJob?.cancel()
        scheduleLaserRemoval(finishedStroke)
        viewModelScope.launch {
            runCatching { drawingRepository.finishStroke(roomId, uid, finishedStroke) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message ?: "낙서를 동기화하지 못했어요")
                }
        }
    }

    fun clearCanvas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                localActiveStroke = null,
                localPendingStrokes = emptyList(),
                error = null,
                savedMessage = null,
            )
            runCatching { drawingRepository.clearCanvas(roomId) }
                .onSuccess { _uiState.value = _uiState.value.copy(savedMessage = "캔버스를 비웠어요") }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "캔버스를 지우지 못했어요") }
        }
    }

    fun undo() {
        val uid = authRepository.currentUser?.uid ?: return
        val pending = _uiState.value.localPendingStrokes
        val latestPending = pending.filter { it.ownerUid == uid }.maxByOrNull { it.createdAt }
        if (latestPending != null) {
            _uiState.value = _uiState.value.copy(
                localPendingStrokes = pending.filterNot { it.strokeId == latestPending.strokeId },
            )
        }
        viewModelScope.launch { drawingRepository.undoLastStroke(roomId, uid) }
    }

    fun addEmoji(emoji: String) {
        val uid = authRepository.currentUser?.uid ?: return
        if (emoji.isBlank()) return
        viewModelScope.launch {
            runCatching { drawingRepository.addEmoji(roomId, uid, emoji.take(4)) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "이모지를 추가하지 못했어요", savedMessage = null) }
        }
    }

    fun undoEmoji() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch { drawingRepository.undoLastEmoji(roomId, uid) }
    }

    fun setBackgroundImage(uri: android.net.Uri) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, savedMessage = null)
            runCatching { drawingRepository.setBackgroundImage(roomId, uid, uri) }
                .onSuccess { _uiState.value = _uiState.value.copy(savedMessage = "배경 이미지를 추가했어요") }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "배경 이미지를 추가하지 못했어요") }
        }
    }

    fun clearBackground() {
        viewModelScope.launch { drawingRepository.clearBackground(roomId) }
    }

    fun setZoom(zoom: Float, panX: Float, panY: Float) {
        _uiState.value = _uiState.value.copy(zoom = zoom.coerceIn(0.7f, 4f), panX = panX, panY = panY)
    }

    fun resetZoom() {
        _uiState.value = _uiState.value.copy(zoom = 1f, panX = 0f, panY = 0f)
    }

    fun saveSnapshot(context: Context, caption: String? = null) {
        val uid = authRepository.currentUser?.uid ?: return
        val state = _uiState.value
        val strokes = state.renderedFinishedStrokes()
        if (strokes.isEmpty() && state.emojis.isEmpty() && state.background == null) {
            _uiState.value = _uiState.value.copy(error = "저장할 낙서가 아직 없어요", savedMessage = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingSnapshot = true, error = null, savedMessage = null)
            runCatching {
                val bitmap = withContext(Dispatchers.IO) {
                    renderSnapshotBitmap(strokes, state.emojis, state.background)
                }
                val localPreviewPath = if (widgetStateStore.shouldUpdateRoom(roomId)) {
                    widgetStateStore.saveLatestDrawingPreview(roomId, bitmap)
                } else {
                    null
                }
                drawingRepository.saveSnapshot(roomId, uid, bitmap, caption, context.applicationContext) to localPreviewPath
            }.onSuccess { (snapshot, localPreviewPath) ->
                if (widgetStateStore.shouldUpdateRoom(roomId)) {
                    widgetStateStore.updateLatestDrawing(roomId, snapshot.imageUrl, localPreviewPath, afterSave = ::updateCoupleWidgets)
                }
                _uiState.value = _uiState.value.copy(
                    isSavingSnapshot = false,
                    savedMessage = "최근 낙서로 저장했어요",
                    error = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingSnapshot = false,
                    savedMessage = null,
                    error = error.toSnapshotMessage(),
                )
            }
        }
    }

    private fun pointKey(): String = "%05d".format(pointIndex++)

    private fun DrawingUiState.renderedFinishedStrokes(): List<Stroke> {
        val remoteIds = strokes.mapTo(mutableSetOf()) { it.strokeId }
        return (strokes + localPendingStrokes.filterNot { it.strokeId in remoteIds })
            .filterNot { it.isExpired(nowMillis) }
    }

    private fun List<Stroke>.dedupPendingStrokes(): List<Stroke> =
        distinctBy { it.strokeId }.filterNot { it.isExpired() }.sortedBy { it.createdAt }

    private fun DrawingUiState.hasLaserStroke(): Boolean =
        strokes.any { it.expiresAt > 0L } ||
            localPendingStrokes.any { it.expiresAt > 0L } ||
            activeStrokes.any { it.expiresAt > 0L } ||
            localActiveStroke?.expiresAt?.let { it > 0L } == true

    private fun Stroke.refreshedLaserExpiry(): Stroke {
        if (expiresAt <= 0L) return this
        return copy(expiresAt = System.currentTimeMillis() + Stroke.LASER_TTL_MS)
    }

    private fun scheduleLaserRemoval(stroke: Stroke) {
        if (stroke.expiresAt <= 0L) return
        viewModelScope.launch {
            delay((stroke.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) + 80L)
            _uiState.value = _uiState.value.copy(
                localPendingStrokes = _uiState.value.localPendingStrokes.filterNot { it.strokeId == stroke.strokeId },
                strokes = _uiState.value.strokes.filterNot { it.strokeId == stroke.strokeId },
                nowMillis = System.currentTimeMillis(),
            )
            runCatching { drawingRepository.removeStroke(roomId, stroke.strokeId) }
        }
    }

    private fun List<Stroke>.withReceiverLocalLaserExpiry(uid: String?, now: Long): List<Stroke> =
        also { strokes ->
            val currentStrokeIds = strokes.mapTo(mutableSetOf()) { it.strokeId }
            remoteLaserExpiries.entries.removeAll { (strokeId, expiry) ->
                expiry.localExpiresAt <= now || strokeId !in currentStrokeIds
            }
        }.map { stroke ->
            if (stroke.expiresAt <= 0L || stroke.ownerUid == uid) return@map stroke
            val cached = remoteLaserExpiries[stroke.strokeId]
            if (cached != null && cached.sourceExpiresAt >= stroke.expiresAt) {
                return@map stroke.copy(expiresAt = cached.localExpiresAt)
            }
            val senderRemaining = stroke.expiresAt - now
            val localTtl = if (senderRemaining in 1..Stroke.LASER_TTL_MS) {
                senderRemaining
            } else {
                Stroke.LASER_TTL_MS
            }
            val localExpiresAt = now + localTtl
            remoteLaserExpiries[stroke.strokeId] = RemoteLaserExpiry(
                sourceExpiresAt = stroke.expiresAt,
                localExpiresAt = localExpiresAt,
            )
            stroke.copy(expiresAt = localExpiresAt)
        }

    private fun cleanupExpiredOwnStrokes(strokes: List<Stroke>, uid: String, now: Long) {
        if (strokes.none { it.ownerUid == uid && it.isExpired(now) }) return
        viewModelScope.launch {
            runCatching { drawingRepository.deleteExpiredStrokes(roomId, uid, now) }
        }
    }

    private fun sendActiveThrottled(force: Boolean = false) {
        val uid = authRepository.currentUser?.uid ?: return
        val stroke = _uiState.value.localActiveStroke ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastSentAt < 45L) return
        lastSentAt = now
        activeSendJob?.cancel()
        activeSendJob = viewModelScope.launch { drawingRepository.updateActiveStroke(roomId, uid, stroke) }
    }

    override fun onCleared() {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { roomRepository.updateOnlineStatus(roomId, uid, false) }
            }
        }
        super.onCleared()
    }

    private fun renderSnapshotBitmap(
        strokes: List<Stroke>,
        emojis: List<CanvasEmoji>,
        background: DrawingBackground?,
        width: Int = 1200,
        height: Int = 1600,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundColor = Color.rgb(255, 255, 255)
        canvas.drawColor(backgroundColor)
        loadBackgroundBitmap(background?.imageUrl)?.let { backgroundBitmap ->
            try {
                canvas.drawBitmap(
                    backgroundBitmap,
                    null,
                    android.graphics.Rect(0, 0, width, height),
                    Paint(Paint.ANTI_ALIAS_FLAG),
                )
            } finally {
                backgroundBitmap.recycle()
            }
        }
        val strokeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeBitmap)
        strokes.forEach { stroke ->
            val points = stroke.sortedPoints()
            if (points.isEmpty()) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (stroke.eraser) Color.TRANSPARENT else runCatching { Color.parseColor(stroke.color) }.getOrDefault(Color.rgb(34, 34, 34))
                style = Paint.Style.STROKE
                strokeWidth = stroke.width * 1.8f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                xfermode = if (stroke.eraser) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
            }
            if (points.size == 1) {
                val point = points.first()
                strokeCanvas.drawCircle(point.x * width, point.y * height, paint.strokeWidth / 2f, paint.apply { style = Paint.Style.FILL })
                return@forEach
            }
            val path = Path()
            val first = points.first()
            path.moveTo(first.x * width, first.y * height)
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val current = points[index]
                val previousX = previous.x * width
                val previousY = previous.y * height
                val midX = (previousX + current.x * width) / 2f
                val midY = (previousY + current.y * height) / 2f
                path.quadTo(previousX, previousY, midX, midY)
            }
            val last = points.last()
            path.lineTo(last.x * width, last.y * height)
            strokeCanvas.drawPath(path, paint)
        }
        canvas.drawBitmap(strokeBitmap, 0f, 0f, null)
        strokeBitmap.recycle()
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        emojis.forEach { emoji ->
            emojiPaint.textSize = emoji.size * 1.8f
            val x = emoji.x.coerceIn(0f, 1f) * width
            val y = emoji.y.coerceIn(0f, 1f) * height
            val baseline = y - (emojiPaint.descent() + emojiPaint.ascent()) / 2f
            canvas.drawText(emoji.emoji, x, baseline, emojiPaint)
        }
        return bitmap
    }

    private fun loadBackgroundBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            URL(url).openStream().use { stream -> BitmapFactory.decodeStream(stream) }
        }.getOrNull()
    }

    private fun Throwable.toSnapshotMessage(): String {
        val detail = message.orEmpty()
        return when {
            detail.contains("storage", ignoreCase = true) || detail.contains("bucket", ignoreCase = true) ->
                "Storage 설정 필요"
            else -> detail.ifBlank { "낙서 저장에 실패했어요" }
        }
    }
}

private data class RemoteLaserExpiry(
    val sourceExpiresAt: Long,
    val localExpiresAt: Long,
)
