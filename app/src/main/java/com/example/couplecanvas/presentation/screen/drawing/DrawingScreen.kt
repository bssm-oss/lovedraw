package com.example.couplecanvas.presentation.screen.drawing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.couplecanvas.data.model.CanvasEmoji
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.DrawingUiState
import com.example.couplecanvas.data.model.Stroke as DrawingStroke
import com.example.couplecanvas.feature.overlay.CoupleOverlayService
import com.example.couplecanvas.feature.overlay.OverlayPermission
import com.example.couplecanvas.presentation.component.CuteTopBar
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.ButterYellow
import com.example.couplecanvas.presentation.theme.RauschPink
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmCanvas
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface
import kotlinx.coroutines.launch

@Composable
fun DrawingScreen(
    roomId: String,
    roomCode: String?,
    roomTitle: String? = null,
    privacyMode: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val container = LocalAppContainer.current
    val viewModel: DrawingViewModel = viewModel(
        key = "drawing-$roomId",
        factory = ViewModelFactory {
            DrawingViewModel(
                roomId,
                container.authRepository,
                container.roomRepository,
                container.drawingRepository,
                container.widgetStateStore,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingOverlayStart by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(OverlayPermission.canDrawOverlays(context)) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasOverlayNotificationPermission()) }

    fun startScreenOverlay(startDrawing: Boolean) {
        coroutineScope.launch {
            val title = roomTitle ?: roomCode ?: "Couple Canvas"
            container.overlayStateStore.setEnabled(true)
            container.widgetStateStore.selectRoom(
                roomId = roomId,
                roomTitle = title,
                privacyMode = privacyMode,
            )
            context.startForegroundService(
                CoupleOverlayService.showIntent(
                    context = context,
                    roomId = roomId,
                    roomTitle = title,
                    startDrawing = startDrawing,
                ),
            )
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasOverlayPermission = OverlayPermission.canDrawOverlays(context)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted || context.hasOverlayNotificationPermission()
    }

    fun requestScreenOverlay() {
        hasNotificationPermission = context.hasOverlayNotificationPermission()
        hasOverlayPermission = OverlayPermission.canDrawOverlays(context)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingOverlayStart = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!hasOverlayPermission) {
            pendingOverlayStart = true
            overlayPermissionLauncher.launch(OverlayPermission.settingsIntent(context))
            return
        }
        pendingOverlayStart = false
        startScreenOverlay(startDrawing = true)
    }

    LaunchedEffect(roomId, roomTitle, roomCode, privacyMode, hasNotificationPermission, hasOverlayPermission) {
        if (hasNotificationPermission && hasOverlayPermission && !privacyMode) {
            startScreenOverlay(startDrawing = false)
        }
    }

    LaunchedEffect(pendingOverlayStart, hasNotificationPermission, hasOverlayPermission) {
        if (pendingOverlayStart && hasNotificationPermission && hasOverlayPermission) {
            pendingOverlayStart = false
            startScreenOverlay(startDrawing = true)
        }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = context.hasOverlayNotificationPermission()
                hasOverlayPermission = OverlayPermission.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(WarmCanvas)) {
        CuteTopBar(
            title = "오늘의 낙서",
            subtitle = listOfNotNull(
                roomCode ?: "방",
                if (!uiState.isConnected) "재연결 중" else if (!uiState.partnerOnline) "상대방 대기" else null,
            ).joinToString(" · "),
            onBack = onBack,
            action = {
                IconButton(
                    onClick = ::requestScreenOverlay,
                    modifier = Modifier.background(SunshineYellow, CircleShape),
                ) {
                    Icon(Icons.Rounded.Brush, contentDescription = "화면 위에 그리기", tint = WarmBlack)
                }
            },
        )
        DrawingCanvas(
            uiState = uiState,
            onStart = viewModel::startStroke,
            onMove = viewModel::appendPoint,
            onEnd = viewModel::finishStroke,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(WarmSurface)
                .border(1.dp, Sand, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ColorPickerRow(selected = uiState.brush.color, onSelect = viewModel::setBrushColor)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolButton(
                    selected = uiState.brush.eraser,
                    icon = Icons.Rounded.Remove,
                    label = "지우기",
                    onClick = viewModel::toggleEraser,
                )
            }
            uiState.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = RauschPink) }
        }
    }
}

@Composable
fun DrawingCanvas(
    uiState: DrawingUiState,
    onStart: (Float, Float) -> Unit,
    onMove: (Float, Float) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(WarmSurface, RoundedCornerShape(28.dp))
            .border(1.dp, Sand, RoundedCornerShape(28.dp))
            .padding(2.dp),
    ) {
        val gestureModifier = Modifier.pointerInput(Unit) {
            fun Offset.toNormalized(): Pair<Float, Float> {
                val canvasWidth = size.width.toFloat().coerceAtLeast(1f)
                val canvasHeight = size.height.toFloat().coerceAtLeast(1f)
                return (x / canvasWidth).coerceIn(0f, 1f) to (y / canvasHeight).coerceIn(0f, 1f)
            }

            detectDragGestures(
                onDragStart = { offset ->
                    val (x, y) = offset.toNormalized()
                    onStart(x, y)
                },
                onDragEnd = onEnd,
                onDragCancel = onEnd,
                onDrag = { change, _ ->
                    val (x, y) = change.position.toNormalized()
                    onMove(x, y)
                },
            )
        }
        drawBackgroundImageLayer(uiState)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier),
        ) {
            val remoteIds = uiState.strokes.mapTo(mutableSetOf()) { it.strokeId }
            val allStrokes = uiState.strokes +
                uiState.localPendingStrokes.filterNot { it.strokeId in remoteIds } +
                uiState.activeStrokes +
                listOfNotNull(uiState.localActiveStroke)
            val visibleStrokes = allStrokes.filterNot { it.isExpired(uiState.nowMillis) }
            withTransform({
                translate(uiState.panX, uiState.panY)
                scale(uiState.zoom, uiState.zoom, pivot = Offset(size.width / 2f, size.height / 2f))
            }) {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                    visibleStrokes.forEach { stroke ->
                        drawStroke(stroke, size.width, size.height, uiState.nowMillis)
                    }
                    uiState.emojis.forEach { emoji ->
                        drawEmoji(emoji, size.width, size.height)
                    }
                    canvas.restore()
                }
            }
        }
    }
}

@Composable
private fun BoxScope.drawBackgroundImageLayer(uiState: DrawingUiState) {
    uiState.background?.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
        AsyncImage(
            model = imageUrl,
            contentDescription = "드로잉 배경 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = uiState.zoom
                    scaleY = uiState.zoom
                    translationX = uiState.panX
                    translationY = uiState.panY
                    transformOrigin = TransformOrigin.Center
                },
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: DrawingStroke, width: Float, height: Float, nowMillis: Long) {
    val points = stroke.sortedPoints()
    if (points.isEmpty()) return
    if (stroke.isExpired(nowMillis)) return
    val blendMode = if (stroke.eraser) BlendMode.Clear else BlendMode.SrcOver
    val color = if (stroke.eraser) Color.Transparent else parseColor(stroke.color).copy(alpha = stroke.alpha(nowMillis))
    if (points.size == 1) {
        val p = points.first().toOffset(width, height)
        drawCircle(color, radius = stroke.width / 2f, center = p, blendMode = blendMode)
        return
    }
    val path = Path()
    val first = points.first().toOffset(width, height)
    path.moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val previous = points[i - 1].toOffset(width, height)
        val current = points[i].toOffset(width, height)
        val mid = Offset((previous.x + current.x) / 2f, (previous.y + current.y) / 2f)
        path.quadraticTo(previous.x, previous.y, mid.x, mid.y)
    }
    val last = points.last().toOffset(width, height)
    path.lineTo(last.x, last.y)
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = blendMode,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmoji(emoji: CanvasEmoji, width: Float, height: Float) {
    if (emoji.emoji.isBlank()) return
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = emoji.size
        }
        val x = emoji.x.coerceIn(0f, 1f) * width
        val y = emoji.y.coerceIn(0f, 1f) * height
        val baseline = y - (paint.descent() + paint.ascent()) / 2f
        canvas.nativeCanvas.drawText(emoji.emoji, x, baseline, paint)
    }
}

private fun DrawingPoint.toOffset(width: Float, height: Float): Offset = Offset(x * width, y * height)

private fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(WarmBlack)

private fun Context.hasOverlayNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
fun ColorPickerRow(selected: String, onSelect: (String) -> Unit) {
    val colors = listOf(
        "#E5484D" to Color(0xFFE5484D),
        "#1E63D6" to Color(0xFF1E63D6),
        "#222222" to WarmBlack,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { (hex, color) ->
            Box(
                Modifier
                    .size(44.dp)
                    .background(color, CircleShape)
                    .border(if (selected == hex) 3.dp else 1.dp, if (selected == hex) WarmBlack else Sand, CircleShape)
                    .clickable { onSelect(hex) },
            )
        }
    }
}

@Composable
private fun ToolButton(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.background(if (selected) ButterYellow else Sand, CircleShape),
        ) {
            Icon(icon, contentDescription = label, tint = WarmBlack.copy(alpha = if (enabled) 1f else 0.32f))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = WarmGray)
    }
}
