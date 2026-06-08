package com.example.couplecanvas.presentation.screen.drawing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.couplecanvas.feature.overlay.CoupleOverlayService
import com.example.couplecanvas.feature.overlay.OverlayPermission
import com.example.couplecanvas.presentation.component.RoundedPastelButton
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.component.SoftCard
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.Mint
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
            val title = roomTitle ?: roomCode ?: "lovedraw"
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

    fun requestScreenOverlay(startDrawing: Boolean) {
        hasNotificationPermission = context.hasOverlayNotificationPermission()
        hasOverlayPermission = OverlayPermission.canDrawOverlays(context)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingOverlayStart = startDrawing
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!hasOverlayPermission) {
            pendingOverlayStart = startDrawing
            overlayPermissionLauncher.launch(OverlayPermission.settingsIntent(context))
            return
        }
        pendingOverlayStart = false
        startScreenOverlay(startDrawing = startDrawing)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCanvas)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("화면 위에 그리기", style = MaterialTheme.typography.titleLarge, color = WarmBlack)
                        Text(
                            roomCode?.let { "방 코드 $it" } ?: "선택한 방",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmGray,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SunshineYellow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = WarmBlack)
                    }
                }

                StatusRow(
                    connected = uiState.isConnected,
                    partnerOnline = uiState.partnerOnline,
                    privacyMode = privacyMode,
                )

                RoundedPastelButton(
                    text = "그리기 시작",
                    onClick = { requestScreenOverlay(startDrawing = true) },
                    enabled = !privacyMode,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryPastelButton(
                    text = "전체 지우기",
                    onClick = viewModel::clearCanvas,
                    enabled = !privacyMode,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = WarmBlack)
                    Text("알림에서 켜고 끌 수 있어요", style = MaterialTheme.typography.titleMedium, color = WarmBlack)
                }
                Text(
                    "홈 화면이나 다른 앱에서도 상단 알림의 그리기 시작 / 그리기 끄기를 사용하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
                Text(
                    "빨강, 파랑, 검정 마커와 굵기 조절은 그리기 중 하단 도구에서 바꿀 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
        }

        uiState.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = RauschPink)
        }
        Spacer(Modifier.height(1.dp))
    }

}

@Composable
private fun StatusRow(
    connected: Boolean,
    partnerOnline: Boolean,
    privacyMode: Boolean,
) {
    val (label, color) = when {
        privacyMode -> "비공개 모드" to RauschPink
        !connected -> "재연결 중" to RauschPink
        partnerOnline -> "연결됨" to Mint
        else -> "상대 대기 중" to WarmGray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WarmSurface)
            .border(1.dp, Sand, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = WarmBlack, fontWeight = FontWeight.SemiBold)
    }
}

private fun Context.hasOverlayNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
