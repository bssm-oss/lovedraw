package com.example.couplecanvas.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.couplecanvas.CoupleCanvasApplication
import com.example.couplecanvas.MainActivity
import com.example.couplecanvas.R
import com.example.couplecanvas.data.local.OverlayStateStore
import com.example.couplecanvas.data.local.WidgetStateStore
import com.example.couplecanvas.data.model.BrushState
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.data.repository.AuthRepository
import com.example.couplecanvas.data.repository.DrawingRepository
import com.example.couplecanvas.presentation.navigation.EXTRA_TARGET_ROOM_ID
import com.example.couplecanvas.presentation.navigation.EXTRA_TARGET_TAB_INDEX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class CoupleOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var overlayStateStore: OverlayStateStore
    private lateinit var widgetStateStore: WidgetStateStore
    private lateinit var authRepository: AuthRepository
    private lateinit var drawingRepository: DrawingRepository

    private var overlayView: View? = null
    private var canvasParams: WindowManager.LayoutParams? = null
    private var drawingCanvasView: ScreenDrawingOverlayView? = null

    private var currentRoomId: String = ""
    private var currentRoomTitle: String = ""
    private var requestedRoomId: String = ""
    private var requestedRoomTitle: String = ""
    private var privacyMode: Boolean = false
    private var finishedStrokes: List<Stroke> = emptyList()
    private var activeStrokes: List<Stroke> = emptyList()
    private var localPendingStrokes: List<Stroke> = emptyList()
    private var localActiveStroke: Stroke? = null
    private var brush = BrushState()
    private var drawingMode: Boolean = false
    private var pointIndex = 0
    private var lastSentAt = 0L
    private val remoteLaserExpiries = mutableMapOf<String, RemoteLaserExpiry>()

    private var snapshotJob: Job? = null
    private var drawingJob: Job? = null
    private var activeSendJob: Job? = null
    private var observedDrawingRoomId: String = ""

    override fun onCreate() {
        super.onCreate()
        val appContainer = (application as CoupleCanvasApplication).container
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayStateStore = appContainer.overlayStateStore
        widgetStateStore = appContainer.widgetStateStore
        authRepository = appContainer.authRepository
        drawingRepository = appContainer.drawingRepository
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    stopOverlay(updateEnabledState = false)
                    overlayStateStore.setEnabled(false)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_DRAWING -> {
                setDrawingMode(!drawingMode)
                return START_STICKY
            }

            ACTION_UNDO -> {
                undoLastStroke()
                return START_STICKY
            }

            ACTION_CLEAR -> {
                clearCanvas()
                return START_STICKY
            }
        }

        if (!OverlayPermission.canDrawOverlays(this)) {
            scope.launch { overlayStateStore.setEnabled(false) }
            stopSelf()
            return START_NOT_STICKY
        }

        requestedRoomId = intent?.getStringExtra(EXTRA_OVERLAY_ROOM_ID).orEmpty()
        requestedRoomTitle = intent?.getStringExtra(EXTRA_OVERLAY_ROOM_TITLE).orEmpty()
        if (requestedRoomId.isNotBlank()) {
            currentRoomId = requestedRoomId
            currentRoomTitle = requestedRoomTitle.ifBlank { "둘만의 그림방" }
        }

        startOverlayForeground()
        showOverlay()
        observeSnapshot()
        observeDrawingRoom(currentRoomId, privacyMode)
        scope.launch { overlayStateStore.setEnabled(true) }
        if (intent?.getBooleanExtra(EXTRA_START_DRAWING, false) == true) {
            setDrawingMode(true)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopOverlay(updateEnabledState = false)
        scope.cancel()
        super.onDestroy()
    }

    private fun startOverlayForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        val view = createOverlayView()
        val params = canvasLayoutParams()
        windowManager.addView(view, params)
        overlayView = view
        canvasParams = params
        renderOverlay()
    }

    private fun canvasLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            canvasFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private fun canvasFlags(): Int {
        val base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return if (drawingMode) base else base or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }

    private fun observeSnapshot() {
        if (snapshotJob != null) return
        snapshotJob = scope.launch {
            widgetStateStore.snapshot.collect { snapshot ->
                currentRoomId = snapshot.roomId.ifBlank { requestedRoomId }
                currentRoomTitle = snapshot.roomTitle.ifBlank { requestedRoomTitle.ifBlank { "Couple Canvas" } }
                privacyMode = snapshot.privacyMode
                observeDrawingRoom(currentRoomId, snapshot.privacyMode)
                renderOverlay()
                updateNotification()
            }
        }
    }

    private fun observeDrawingRoom(roomId: String, privacyMode: Boolean) {
        if (roomId.isBlank() || privacyMode) {
            observedDrawingRoomId = ""
            drawingJob?.cancel()
            drawingJob = null
            finishedStrokes = emptyList()
            activeStrokes = emptyList()
            localPendingStrokes = emptyList()
            localActiveStroke = null
            renderOverlay()
            return
        }
        if (observedDrawingRoomId == roomId && drawingJob?.isActive == true) return
        observedDrawingRoomId = roomId
        drawingJob?.cancel()
        drawingJob = scope.launch {
            launch {
                drawingRepository.observeFinishedStrokes(roomId).collect { strokes ->
                    val now = System.currentTimeMillis()
                    val uid = authRepository.currentUser?.uid
                    val sorted = strokes
                        .withReceiverLocalLaserExpiry(uid, now)
                        .filterNot { it.isExpired(now) }
                        .sortedBy { it.createdAt }
                    val remoteIds = sorted.mapTo(mutableSetOf()) { it.strokeId }
                    finishedStrokes = sorted
                    localPendingStrokes = localPendingStrokes.filterNot { it.strokeId in remoteIds || it.isExpired(now) }
                    uid?.let { localUid ->
                        if (strokes.any { it.ownerUid == localUid && it.isExpired(now) }) {
                            launch { runCatching { drawingRepository.deleteExpiredStrokes(roomId, localUid, now) } }
                        }
                    }
                    renderOverlay()
                }
            }
            launch {
                drawingRepository.observeActiveStrokes(roomId).collect { strokes ->
                    val uid = authRepository.currentUser?.uid
                    val now = System.currentTimeMillis()
                    activeStrokes = strokes
                        .filter { it.ownerUid != uid }
                        .withReceiverLocalLaserExpiry(uid, now)
                        .filterNot { it.isExpired(now) }
                        .sortedBy { it.createdAt }
                    renderOverlay()
                }
            }
            launch {
                while (isActive) {
                    runCatching {
                        val uid = authRepository.currentUser?.uid
                        val now = System.currentTimeMillis()
                        drawingRepository.deleteExpiredStrokes(roomId, uid.orEmpty(), now)
                        finishedStrokes = drawingRepository.getFinishedStrokes(roomId)
                            .withReceiverLocalLaserExpiry(uid, now)
                            .filterNot { it.isExpired(now) }
                            .sortedBy { it.createdAt }
                        activeStrokes = drawingRepository.getActiveStrokes(roomId)
                            .filter { it.ownerUid != uid }
                            .withReceiverLocalLaserExpiry(uid, now)
                            .filterNot { it.isExpired(now) }
                            .sortedBy { it.createdAt }
                        val remoteIds = finishedStrokes.mapTo(mutableSetOf()) { it.strokeId }
                        localPendingStrokes = localPendingStrokes.filterNot { it.strokeId in remoteIds || it.isExpired(now) }
                        renderOverlay()
                    }
                    delay(1_500)
                }
            }
        }
    }

    private fun createOverlayView(): View {
        return ScreenDrawingOverlayView(this).apply {
            onStartStroke = ::startStroke
            onMoveStroke = ::appendPoint
            onEndStroke = ::finishStroke
            onBrushColorSelected = ::selectBrushColor
            onBrushWidthSelected = ::selectBrushWidth
            onClearSelected = ::clearCanvas
            onCloseDrawing = { setDrawingMode(false) }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            drawingCanvasView = this
        }
    }

    private fun setDrawingMode(enabled: Boolean) {
        drawingMode = enabled && currentRoomId.isNotBlank() && !privacyMode && authRepository.currentUser != null
        canvasParams?.let { params ->
            params.flags = canvasFlags()
            overlayView?.let { view -> runCatching { windowManager.updateViewLayout(view, params) } }
        }
        if (!drawingMode) {
            localActiveStroke = null
            activeSendJob?.cancel()
            clearOwnActiveStroke()
        }
        renderOverlay()
        updateNotification()
    }

    private fun selectBrushColor(color: String) {
        brush = brush.copy(color = color, eraser = false)
        renderOverlay()
        updateNotification()
    }

    private fun selectBrushWidth(width: Float) {
        brush = brush.copy(width = width.coerceIn(4f, 32f), eraser = false)
        renderOverlay()
        updateNotification()
    }

    private fun startStroke(x: Float, y: Float) {
        val uid = authRepository.currentUser?.uid ?: return
        val roomId = currentRoomId.takeIf { it.isNotBlank() && !privacyMode } ?: return
        pointIndex = 0
        val now = System.currentTimeMillis()
        val point = DrawingPoint(x, y, now)
        localActiveStroke = Stroke(
            strokeId = UUID.randomUUID().toString(),
            ownerUid = uid,
            color = brush.color,
            width = brush.width,
            eraser = false,
            createdAt = now,
            expiresAt = now + Stroke.LASER_TTL_MS,
            points = mapOf(pointKey() to point),
        )
        sendActiveStroke(roomId, uid, force = true)
        renderOverlay()
    }

    private fun appendPoint(x: Float, y: Float) {
        val stroke = localActiveStroke ?: return
        val now = System.currentTimeMillis()
        localActiveStroke = stroke.copy(
            points = stroke.points + (pointKey() to DrawingPoint(x, y, now)),
            expiresAt = now + Stroke.LASER_TTL_MS,
        )
        val uid = authRepository.currentUser?.uid ?: return
        val roomId = currentRoomId.takeIf { it.isNotBlank() && !privacyMode } ?: return
        sendActiveStroke(roomId, uid, force = false)
        renderOverlay()
    }

    private fun finishStroke() {
        val uid = authRepository.currentUser?.uid ?: return
        val roomId = currentRoomId.takeIf { it.isNotBlank() && !privacyMode } ?: return
        val stroke = localActiveStroke ?: return
        val finishedStroke = stroke.refreshedLaserExpiry()
        localActiveStroke = null
        localPendingStrokes = (localPendingStrokes + finishedStroke)
            .distinctBy { it.strokeId }
            .filterNot { it.isExpired() }
            .sortedBy { it.createdAt }
        activeSendJob?.cancel()
        renderOverlay()
        updateNotification()
        scheduleLaserRemoval(roomId, finishedStroke)
        scope.launch {
            runCatching { drawingRepository.finishStroke(roomId, uid, finishedStroke) }
        }
    }

    private fun sendActiveStroke(roomId: String, uid: String, force: Boolean) {
        val stroke = localActiveStroke ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastSentAt < 45L) return
        lastSentAt = now
        activeSendJob?.cancel()
        activeSendJob = scope.launch {
            runCatching { drawingRepository.updateActiveStroke(roomId, uid, stroke) }
        }
    }

    private fun clearOwnActiveStroke() {
        val uid = authRepository.currentUser?.uid ?: return
        val roomId = currentRoomId.takeIf { it.isNotBlank() && !privacyMode } ?: return
        scope.launch { runCatching { drawingRepository.clearActiveStroke(roomId, uid) } }
    }

    private fun clearCanvas() {
        val roomId = currentRoomId.takeIf { it.isNotBlank() && !privacyMode } ?: return
        localActiveStroke = null
        localPendingStrokes = emptyList()
        finishedStrokes = emptyList()
        activeStrokes = emptyList()
        renderOverlay()
        updateNotification()
        scope.launch { runCatching { drawingRepository.clearCanvas(roomId) } }
    }

    private fun undoLastStroke() {
        val uid = authRepository.currentUser?.uid ?: return
        val roomId = currentRoomId.takeIf { it.isNotBlank() && !privacyMode } ?: return
        val latestPending = localPendingStrokes.filter { it.ownerUid == uid }.maxByOrNull { it.createdAt }
        if (latestPending != null) {
            localPendingStrokes = localPendingStrokes.filterNot { it.strokeId == latestPending.strokeId }
            renderOverlay()
        }
        updateNotification()
        scope.launch { runCatching { drawingRepository.undoLastStroke(roomId, uid) } }
    }

    private fun renderOverlay() {
        val uid = authRepository.currentUser?.uid
        val remoteIds = finishedStrokes.mapTo(mutableSetOf()) { it.strokeId }
        val now = System.currentTimeMillis()
        val allStrokes = (finishedStrokes +
            localPendingStrokes.filterNot { it.strokeId in remoteIds } +
            activeStrokes.filter { it.ownerUid != uid } +
            listOfNotNull(localActiveStroke))
            .filterNot { it.isExpired(now) }
        val canDraw = currentRoomId.isNotBlank() && !privacyMode && uid != null
        drawingCanvasView?.setOverlayContent(
            strokes = allStrokes,
            brush = brush,
            drawingEnabled = canDraw && drawingMode,
        )
    }

    private fun stopOverlay(updateEnabledState: Boolean) {
        snapshotJob?.cancel()
        snapshotJob = null
        drawingJob?.cancel()
        drawingJob = null
        activeSendJob?.cancel()
        activeSendJob = null
        observedDrawingRoomId = ""
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        canvasParams = null
        drawingCanvasView = null
        drawingMode = false
        if (updateEnabledState) {
            scope.launch { overlayStateStore.setEnabled(false) }
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (currentRoomId.isNotBlank()) {
                putExtra(EXTRA_TARGET_ROOM_ID, currentRoomId)
                putExtra(EXTRA_TARGET_TAB_INDEX, 0)
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(this, CoupleOverlayService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleIntent = Intent(this, CoupleOverlayService::class.java).setAction(ACTION_TOGGLE_DRAWING)
        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val clearIntent = Intent(this, CoupleOverlayService::class.java).setAction(ACTION_CLEAR)
        val clearPendingIntent = PendingIntent.getService(
            this,
            3,
            clearIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val drawingActionLabel = if (drawingMode) "그리기 끄기" else "그리기 시작"
        val drawingActionIcon = if (drawingMode) android.R.drawable.ic_media_pause else android.R.drawable.ic_menu_edit
        val now = System.currentTimeMillis()
        val title = currentRoomTitle.ifBlank { "Couple Canvas" }
        val contentText = if (drawingMode) {
            "그리기 켜짐"
        } else {
            "그리기 꺼짐"
        }
        val bigText = "$title\n$contentText"

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(Notification.PRIORITY_LOW)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, drawingActionIcon),
                    drawingActionLabel,
                    togglePendingIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_delete),
                    "전체 지우기",
                    clearPendingIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "종료",
                    stopPendingIntent,
                ).build(),
            )
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "화면 위 그리기",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "알림에서 화면 위 그리기를 켜고 끄는 도구"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun pointKey(): String = "%05d".format(pointIndex++)

    private fun Stroke.refreshedLaserExpiry(): Stroke {
        if (expiresAt <= 0L) return this
        return copy(expiresAt = System.currentTimeMillis() + Stroke.LASER_TTL_MS)
    }

    private fun scheduleLaserRemoval(roomId: String, stroke: Stroke) {
        if (stroke.expiresAt <= 0L) return
        scope.launch {
            delay((stroke.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) + 80L)
            localPendingStrokes = localPendingStrokes.filterNot { it.strokeId == stroke.strokeId }
            finishedStrokes = finishedStrokes.filterNot { it.strokeId == stroke.strokeId }
            renderOverlay()
            updateNotification()
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

    companion object {
        const val ACTION_SHOW = "com.example.couplecanvas.overlay.SHOW"
        const val ACTION_STOP = "com.example.couplecanvas.overlay.STOP"
        const val ACTION_TOGGLE_DRAWING = "com.example.couplecanvas.overlay.TOGGLE_DRAWING"
        const val ACTION_UNDO = "com.example.couplecanvas.overlay.UNDO"
        const val ACTION_CLEAR = "com.example.couplecanvas.overlay.CLEAR"
        private const val EXTRA_OVERLAY_ROOM_ID = "com.example.couplecanvas.overlay.ROOM_ID"
        private const val EXTRA_OVERLAY_ROOM_TITLE = "com.example.couplecanvas.overlay.ROOM_TITLE"
        private const val EXTRA_START_DRAWING = "com.example.couplecanvas.overlay.START_DRAWING"
        private const val CHANNEL_ID = "couple_canvas_overlay"
        private const val NOTIFICATION_ID = 4132

        fun showIntent(
            context: Context,
            roomId: String? = null,
            roomTitle: String? = null,
            startDrawing: Boolean = false,
        ): Intent =
            Intent(context, CoupleOverlayService::class.java)
                .setAction(ACTION_SHOW)
                .putExtra(EXTRA_OVERLAY_ROOM_ID, roomId.orEmpty())
                .putExtra(EXTRA_OVERLAY_ROOM_TITLE, roomTitle.orEmpty())
                .putExtra(EXTRA_START_DRAWING, startDrawing)

        fun stopIntent(context: Context): Intent =
            Intent(context, CoupleOverlayService::class.java).setAction(ACTION_STOP)
    }
}

private data class RemoteLaserExpiry(
    val sourceExpiresAt: Long,
    val localExpiresAt: Long,
)
