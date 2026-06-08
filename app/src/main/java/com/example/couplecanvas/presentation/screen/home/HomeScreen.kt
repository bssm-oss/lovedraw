package com.example.couplecanvas.presentation.screen.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.couplecanvas.data.model.RoomHomeSummary
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.feature.overlay.CoupleOverlayService
import com.example.couplecanvas.feature.overlay.OverlayPermission
import com.example.couplecanvas.presentation.component.EmptyState
import com.example.couplecanvas.presentation.component.LegalLinksCard
import com.example.couplecanvas.presentation.component.RoundedPastelButton
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.ViewModelFactory
import com.example.couplecanvas.presentation.theme.Mint
import com.example.couplecanvas.presentation.theme.RauschPink
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.SunshineYellowDeep
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmCanvas
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface
import com.example.couplecanvas.util.ConnectionDisplayState
import com.example.couplecanvas.util.OverlayQuickStartCopy
import com.example.couplecanvas.util.connectionDisplayState
import com.example.couplecanvas.util.StatsCalculator
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    initialJoinCode: String? = null,
    onInviteConsumed: () -> Unit = {},
    onOpenRoom: (String) -> Unit,
    onWaitRoom: (String) -> Unit,
    onSignedOut: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory {
            HomeViewModel(container.authRepository, container.roomRepository, container.widgetStateStore)
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var pendingLeaveSummary by remember { mutableStateOf<RoomHomeSummary?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val overlayEnabled by container.overlayStateStore.enabled.collectAsStateWithLifecycle(initialValue = false)
    var hasOverlayPermission by remember { mutableStateOf(OverlayPermission.canDrawOverlays(context)) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasOverlayNotificationPermission()) }
    var pendingOverlayStart by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasOverlayPermission = OverlayPermission.canDrawOverlays(context)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted || context.hasOverlayNotificationPermission()
    }
    val feedback = uiState.error ?: uiState.message
    val activeSummaries = uiState.roomSummaries
        .filterNot { it.room.status == RoomStatus.Closed.value }
        .sortedByDescending { it.room.updatedAt }
    val archivedSummaries = uiState.roomSummaries
        .filter { it.room.status == RoomStatus.Closed.value }
        .sortedByDescending { it.room.updatedAt }
    val overlaySummary = activeSummaries.firstOrNull() ?: uiState.roomSummaries.maxByOrNull { it.room.updatedAt }

    fun startOverlay(summary: RoomHomeSummary?) {
        if (!context.hasOverlayNotificationPermission()) {
            pendingOverlayStart = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        hasNotificationPermission = true
        if (!OverlayPermission.canDrawOverlays(context)) {
            hasOverlayPermission = false
            overlayPermissionLauncher.launch(OverlayPermission.settingsIntent(context))
            return
        }
        if (summary == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("방이 필요해요") }
            return
        }
        coroutineScope.launch {
            container.widgetStateStore.selectRoom(
                roomId = summary.room.roomId,
                roomTitle = summary.room.title,
                privacyMode = summary.room.privacyMode,
            )
            context.startForegroundService(
                CoupleOverlayService.showIntent(
                    context = context,
                    roomId = summary.room.roomId,
                    roomTitle = summary.room.title,
                    startDrawing = true,
                ),
            )
        }
    }

    LaunchedEffect(hasNotificationPermission, pendingOverlayStart, overlaySummary?.room?.roomId) {
        if (pendingOverlayStart && hasNotificationPermission) {
            pendingOverlayStart = false
            startOverlay(overlaySummary)
        }
    }
    LaunchedEffect(uiState.createdRoomId) {
        uiState.createdRoomId?.let {
            viewModel.clearNavigation()
            onWaitRoom(it)
        }
    }
    LaunchedEffect(uiState.joinedRoomId) {
        uiState.joinedRoomId?.let {
            viewModel.clearNavigation()
            onOpenRoom(it)
        }
    }
    LaunchedEffect(initialJoinCode) {
        val code = initialJoinCode?.trim()?.uppercase()?.takeIf { it.length == 6 }
        if (code != null) {
            viewModel.joinRoom(code)
            onInviteConsumed()
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            viewModel.clearFeedback()
        }
    }
    LaunchedEffect(overlayEnabled, hasOverlayPermission, overlaySummary?.room?.roomId) {
        val summary = overlaySummary
        if (overlayEnabled && hasOverlayPermission && summary != null) {
            container.widgetStateStore.selectRoom(
                roomId = summary.room.roomId,
                roomTitle = summary.room.title,
                privacyMode = summary.room.privacyMode,
            )
            context.startForegroundService(
                CoupleOverlayService.showIntent(
                    context = context,
                    roomId = summary.room.roomId,
                    roomTitle = summary.room.title,
                ),
            )
        }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = OverlayPermission.canDrawOverlays(context)
                hasNotificationPermission = context.hasOverlayNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = WarmCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCanvas)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HomeHeader(
                    displayName = uiState.displayName,
                    connected = uiState.isFirebaseConnected,
                    onSignOut = {
                        coroutineScope.launch {
                            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                            viewModel.signOut()
                            onSignedOut()
                        }
                    },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    RoundedPastelButton(
                        "새 방 만들기",
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isBusy,
                    )
                    SecondaryPastelButton(
                        "코드 입장",
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isBusy,
                    )
                }
            }
            item {
                OverlayQuickStartCard(
                    summary = overlaySummary,
                    firebaseConnected = uiState.isFirebaseConnected,
                    overlayEnabled = overlayEnabled,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    isBusy = uiState.isBusy,
                    onStart = { startOverlay(overlaySummary) },
                )
            }
            if (uiState.isLoading) {
                item { EmptyState("불러오는 중", "잠시만요") }
            } else if (activeSummaries.isEmpty() && archivedSummaries.isEmpty()) {
                item {
                    EmptyState("아직 방이 없어요", "새 방을 만들거나 초대 코드로 입장하세요.")
                }
            } else {
                if (activeSummaries.isNotEmpty()) {
                    item {
                        Text("내 그림방", style = MaterialTheme.typography.titleMedium, color = WarmBlack)
                    }
                    items(activeSummaries, key = { it.room.roomId }) { summary ->
                        RoomRow(
                            summary = summary,
                            isFirebaseConnected = uiState.isFirebaseConnected,
                            isBusy = uiState.isBusy,
                            onOpen = {
                                if (summary.room.status == RoomStatus.Waiting.value) onWaitRoom(summary.room.roomId)
                                else onOpenRoom(summary.room.roomId)
                            },
                            onWait = { onWaitRoom(summary.room.roomId) },
                            onClose = { viewModel.closeRoom(summary.room.roomId) },
                            onReopen = { viewModel.reopenRoom(summary.room.roomId) },
                            onLeave = { pendingLeaveSummary = summary },
                        )
                    }
                }
                if (archivedSummaries.isNotEmpty()) {
                    item {
                        Text("보관한 방", style = MaterialTheme.typography.titleMedium, color = WarmGray)
                    }
                    items(archivedSummaries, key = { it.room.roomId }) { summary ->
                        RoomRow(
                            summary = summary,
                            isFirebaseConnected = uiState.isFirebaseConnected,
                            isBusy = uiState.isBusy,
                            onOpen = { onOpenRoom(summary.room.roomId) },
                            onWait = { onWaitRoom(summary.room.roomId) },
                            onClose = { viewModel.closeRoom(summary.room.roomId) },
                            onReopen = { viewModel.reopenRoom(summary.room.roomId) },
                            onLeave = { pendingLeaveSummary = summary },
                        )
                    }
                }
            }
            item {
                LegalLinksCard(Modifier.fillMaxWidth())
            }
        }
    }

    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { showCreateDialog = false; viewModel.createRoom(it) },
        )
    }
    if (showJoinDialog) {
        JoinRoomDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { showJoinDialog = false; viewModel.joinRoom(it) },
        )
    }
    pendingLeaveSummary?.let { summary ->
        LeaveRoomDialog(
            roomTitle = summary.room.title,
            onDismiss = { pendingLeaveSummary = null },
            onConfirm = { pendingLeaveSummary = null; viewModel.leaveRoom(summary.room.roomId) },
        )
    }
}

@Composable
private fun OverlayQuickStartCard(
    summary: RoomHomeSummary?,
    firebaseConnected: Boolean,
    overlayEnabled: Boolean,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    isBusy: Boolean,
    onStart: () -> Unit,
) {
    val status = summary?.room?.connectionDisplayState(firebaseConnected)
    val overlayReady = overlayEnabled && summary != null && firebaseConnected
    val body = when {
        summary == null -> OverlayQuickStartCopy.NO_ROOM_BODY
        !firebaseConnected -> OverlayQuickStartCopy.RECONNECTING_BODY
        !hasNotificationPermission -> OverlayQuickStartCopy.NOTIFICATION_BODY
        !hasOverlayPermission -> OverlayQuickStartCopy.OVERLAY_BODY
        overlayReady -> OverlayQuickStartCopy.ENABLED_BODY
        else -> OverlayQuickStartCopy.READY_BODY
    }
    val enabled = summary != null && firebaseConnected && !isBusy
    val buttonText = when {
        summary == null -> OverlayQuickStartCopy.NO_ROOM_BUTTON
        !firebaseConnected -> OverlayQuickStartCopy.RECONNECTING_BUTTON
        overlayReady -> OverlayQuickStartCopy.ENABLED_BUTTON
        else -> OverlayQuickStartCopy.START_BUTTON
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(SunshineYellow.copy(alpha = 0.42f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Brush, contentDescription = null, tint = WarmBlack)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(OverlayQuickStartCopy.TITLE, style = MaterialTheme.typography.titleMedium, color = WarmBlack)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = WarmGray)
                }
                status?.let { ConnectionStatusPill(it) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundedPastelButton(
                    text = buttonText,
                    onClick = onStart,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                if (overlayReady) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Mint.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = "그리기 알림 켜짐", tint = Mint)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(displayName: String, connected: Boolean, onSignOut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("lovedraw", style = MaterialTheme.typography.labelLarge, color = WarmGray)
            Text(
                displayName,
                style = MaterialTheme.typography.titleMedium,
                color = WarmBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (connected) "연결됨" else "재연결 중",
                style = MaterialTheme.typography.bodySmall,
                color = if (connected) Mint else RauschPink,
            )
        }
        IconButton(onClick = onSignOut) {
            Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = "로그아웃", tint = WarmGray)
        }
    }
}

@Composable
private fun RoomRow(
    summary: RoomHomeSummary,
    isFirebaseConnected: Boolean,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onWait: () -> Unit,
    onClose: () -> Unit,
    onReopen: () -> Unit,
    onLeave: () -> Unit,
) {
    val room = summary.room
    val daysTogether = StatsCalculator.daysTogether(room.startedAt)
    val isClosed = room.status == RoomStatus.Closed.value
    var showMenu by remember { mutableStateOf(false) }
    val status = room.connectionDisplayState(isFirebaseConnected)

    Box {
        Card(
            onClick = onOpen,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarmSurface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        room.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            if (daysTogether > 0) append("D+$daysTogether · ")
                            append("${room.roomCode} · ${room.updatedAt.relativeActivityText()}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        status.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            ConnectionDisplayState.Connected -> Mint
                            ConnectionDisplayState.Waiting -> SunshineYellowDeep
                            ConnectionDisplayState.Reconnecting -> RauschPink
                            ConnectionDisplayState.Archived -> WarmGray
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ConnectionStatusPill(status)
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "옵션", tint = WarmGray)
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (!isClosed && room.status == RoomStatus.Waiting.value) {
                DropdownMenuItem(
                    text = { Text("코드 보기") },
                    onClick = { showMenu = false; onWait() },
                )
            }
            if (isClosed) {
                DropdownMenuItem(
                    text = { Text("다시 열기") },
                    onClick = { showMenu = false; onReopen() },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("보관하기") },
                    onClick = { showMenu = false; onClose() },
                )
            }
            DropdownMenuItem(
                text = { Text("방 나가기") },
                onClick = { showMenu = false; onLeave() },
            )
        }
    }
}

@Composable
private fun ConnectionStatusPill(status: ConnectionDisplayState) {
    val background = when (status) {
        ConnectionDisplayState.Connected -> Mint.copy(alpha = 0.26f)
        ConnectionDisplayState.Waiting -> SunshineYellow.copy(alpha = 0.38f)
        ConnectionDisplayState.Reconnecting -> RauschPink.copy(alpha = 0.12f)
        ConnectionDisplayState.Archived -> WarmCanvas
    }
    val content = when (status) {
        ConnectionDisplayState.Connected -> Mint
        ConnectionDisplayState.Waiting -> SunshineYellowDeep
        ConnectionDisplayState.Reconnecting -> RauschPink
        ConnectionDisplayState.Archived -> WarmGray
    }
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .semantics { contentDescription = status.accessibilityLabel }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(content, RoundedCornerShape(999.dp)),
        )
        Text(status.label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

private fun Context.hasOverlayNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.stopOverlayService() {
    stopService(CoupleOverlayService.stopIntent(this))
}

@Composable
fun CreateRoomDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("둘만의 그림방") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 그림방") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(24) },
                label = { Text("방 이름") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(title) }) { Text("만들기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
fun JoinRoomDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("초대 코드 입력") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(6) },
                label = { Text("6자리 코드") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onJoin(code) }) { Text("입장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun LeaveRoomDialog(roomTitle: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("방에서 나갈까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(roomTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("내 목록에서 제거돼요.", color = WarmGray)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("나가기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private fun Long.relativeActivityText(nowMillis: Long = System.currentTimeMillis()): String {
    if (this <= 0L) return "아직 활동 없음"
    val minutes = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(this), Instant.ofEpochMilli(nowMillis)).coerceAtLeast(0)
    return when {
        minutes < 1 -> "방금"
        minutes < 60 -> "${minutes}분 전"
        minutes < 24 * 60 -> "${minutes / 60}시간 전"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)}일 전"
        else -> Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("M월 d일"))
    }
}
