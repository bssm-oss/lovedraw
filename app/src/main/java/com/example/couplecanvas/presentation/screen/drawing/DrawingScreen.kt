package com.example.couplecanvas.presentation.screen.drawing

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.couplecanvas.data.model.CanvasEmoji
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.DrawingUiState
import com.example.couplecanvas.data.model.Stroke as DrawingStroke
import com.example.couplecanvas.presentation.component.CuteTopBar
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.ButterYellow
import com.example.couplecanvas.presentation.theme.Coral
import com.example.couplecanvas.presentation.theme.Lavender
import com.example.couplecanvas.presentation.theme.Mint
import com.example.couplecanvas.presentation.theme.RauschPink
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SkyBlue
import com.example.couplecanvas.presentation.theme.SoftPink
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmCanvas
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface

@Composable
fun DrawingScreen(roomId: String, roomCode: String?, onBack: (() -> Unit)? = null) {
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
    var transformMode by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val backgroundPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setBackgroundImage(uri)
    }

    Column(Modifier.fillMaxSize().background(WarmCanvas)) {
        CuteTopBar(
            title = "오늘의 낙서",
            subtitle = listOfNotNull(
                roomCode ?: "방",
                if (!uiState.isConnected) "재연결 중" else if (!uiState.partnerOnline) "상대방 대기" else null,
                "${uiState.zoom.formatZoom()}x",
            ).joinToString(" · "),
            onBack = onBack,
        )
        DrawingCanvas(
            uiState = uiState,
            transformMode = transformMode,
            onStart = viewModel::startStroke,
            onMove = viewModel::appendPoint,
            onEnd = viewModel::finishStroke,
            onTransform = viewModel::setZoom,
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
            BrushSizePicker(width = uiState.brush.width, onChange = viewModel::setBrushWidth)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolButton(selected = !transformMode, icon = Icons.Rounded.Draw, label = "그리기", onClick = { transformMode = false })
                ToolButton(selected = uiState.brush.laser && !uiState.brush.eraser, icon = Icons.Rounded.Flare, label = "레이저", onClick = viewModel::toggleLaser)
                ToolButton(selected = transformMode, icon = Icons.Rounded.ZoomIn, label = "확대/이동", onClick = { transformMode = true })
                ToolButton(selected = uiState.brush.eraser, icon = Icons.Rounded.Remove, label = "지우개", onClick = viewModel::toggleEraser)
                ToolButton(selected = false, icon = Icons.AutoMirrored.Rounded.Undo, label = "되돌리기", onClick = viewModel::undo)
                ToolButton(selected = false, icon = Icons.Rounded.EmojiEmotions, label = "이모지", onClick = { viewModel.addEmoji("💗") })
                ToolButton(
                    selected = uiState.background != null,
                    icon = Icons.Rounded.Image,
                    label = if (uiState.background == null) "배경" else "배경 변경",
                    enabled = !uiState.isSavingSnapshot,
                    onClick = { backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
                ToolButton(selected = false, icon = Icons.Rounded.Delete, label = "전체 지우기", onClick = { showClearDialog = true })
                ToolButton(selected = false, icon = Icons.Rounded.Save, label = if (uiState.isSavingSnapshot) "저장 중" else "저장", enabled = !uiState.isSavingSnapshot, onClick = {
                    viewModel.saveSnapshot(context, "오늘의 낙서")
                })
                ToolButton(selected = false, icon = Icons.Rounded.ZoomIn, label = "원래 크기", onClick = viewModel::resetZoom)
            }
            EmojiPickerRow(onSelect = viewModel::addEmoji, onUndoEmoji = viewModel::undoEmoji, onClearBackground = viewModel::clearBackground, hasBackground = uiState.background != null)
            uiState.savedMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = RauschPink) }
            uiState.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = RauschPink) }
            uiState.snapshots.firstOrNull()?.let {
                Text("저장 ${uiState.snapshots.size}", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
            }
        }
    }

    if (showClearDialog) {
        ClearCanvasDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                viewModel.clearCanvas()
            },
        )
    }
}

@Composable
private fun ClearCanvasDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("캔버스를 모두 지울까요?") },
        text = {
            Text(
                "현재 캔버스가 비워져요.",
                color = WarmGray,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("지우기", color = RauschPink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
fun DrawingCanvas(
    uiState: DrawingUiState,
    transformMode: Boolean,
    onStart: (Float, Float) -> Unit,
    onMove: (Float, Float) -> Unit,
    onEnd: () -> Unit,
    onTransform: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(WarmSurface, RoundedCornerShape(28.dp))
            .border(1.dp, Sand, RoundedCornerShape(28.dp))
            .padding(2.dp),
    ) {
        val gestureModifier = if (transformMode) {
            Modifier.pointerInput(uiState.zoom, uiState.panX, uiState.panY) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    onTransform(uiState.zoom * zoomChange, uiState.panX + pan.x, uiState.panY + pan.y)
                }
            }
        } else {
            Modifier.pointerInput(uiState.zoom, uiState.panX, uiState.panY) {
                fun Offset.toNormalized(): Pair<Float, Float> {
                    val canvasWidth = size.width.toFloat().coerceAtLeast(1f)
                    val canvasHeight = size.height.toFloat().coerceAtLeast(1f)
                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight / 2f
                    val logicalX = ((x - uiState.panX - centerX) / uiState.zoom) + centerX
                    val logicalY = ((y - uiState.panY - centerY) / uiState.zoom) + centerY
                    return (logicalX / canvasWidth).coerceIn(0f, 1f) to (logicalY / canvasHeight).coerceIn(0f, 1f)
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

@Composable
fun ColorPickerRow(selected: String, onSelect: (String) -> Unit) {
    val colors = listOf(
        "#FF7A9A" to SoftPink,
        "#CBB7FF" to Lavender,
        "#A8E6CF" to Mint,
        "#A9D6FF" to SkyBlue,
        "#FFD45D" to ButterYellow,
        "#FF9A8B" to Coral,
        "#222222" to WarmBlack,
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { (hex, color) ->
            Box(
                Modifier
                    .size(42.dp)
                    .background(color, CircleShape)
                    .border(if (selected == hex) 3.dp else 1.dp, if (selected == hex) WarmBlack else Sand, CircleShape)
                    .clickable { onSelect(hex) },
            )
        }
    }
}

@Composable
fun BrushSizePicker(width: Float, onChange: (Float) -> Unit) {
    Column {
        Text("${width.toInt()}px", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
        Slider(value = width, onValueChange = onChange, valueRange = 3f..28f)
    }
}

@Composable
private fun EmojiPickerRow(
    onSelect: (String) -> Unit,
    onUndoEmoji: () -> Unit,
    onClearBackground: () -> Unit,
    hasBackground: Boolean,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("💗", "⭐", "🌷", "🍀", "☁️", "🎀", "😊").forEach { emoji ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Sand)
                    .clickable { onSelect(emoji) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
            }
        }
        SecondaryMiniTextButton("이모지 취소", onClick = onUndoEmoji)
        if (hasBackground) SecondaryMiniTextButton("배경 제거", onClick = onClearBackground)
    }
}

@Composable
private fun SecondaryMiniTextButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = WarmBlack,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Sand)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
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

private fun Float.formatZoom(): String = "%.1f".format(this)
