package com.example.couplecanvas.presentation.screen.permission

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.couplecanvas.feature.overlay.OverlayPermission
import com.example.couplecanvas.presentation.component.BrandIconTile
import com.example.couplecanvas.presentation.component.RoundedPastelButton
import com.example.couplecanvas.presentation.component.SecondaryPastelButton
import com.example.couplecanvas.presentation.theme.Mint
import com.example.couplecanvas.presentation.theme.Sand
import com.example.couplecanvas.presentation.theme.SunshineYellow
import com.example.couplecanvas.presentation.theme.WarmBlack
import com.example.couplecanvas.presentation.theme.WarmCanvas
import com.example.couplecanvas.presentation.theme.WarmGray
import com.example.couplecanvas.presentation.theme.WarmSurface
import com.example.couplecanvas.presentation.theme.WarmSurfaceAlt
import kotlinx.coroutines.launch

@Composable
fun PermissionOnboardingScreen(onReady: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var notificationGranted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission.canDrawOverlays(context)) }
    var locationGranted by remember { mutableStateOf(context.hasLocationPermission()) }
    var includeLocation by remember { mutableStateOf(false) }
    var pendingFlow by remember { mutableStateOf(false) }
    var overlayAttempted by remember { mutableStateOf(false) }
    var notificationAttempted by remember { mutableStateOf(false) }
    var locationAttempted by remember { mutableStateOf(false) }
    var flowTick by remember { mutableIntStateOf(0) }

    fun refreshPermissions() {
        notificationGranted = context.hasNotificationPermission()
        overlayGranted = OverlayPermission.canDrawOverlays(context)
        locationGranted = context.hasLocationPermission()
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
        flowTick += 1
    }
    val overlaySettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshPermissions()
        flowTick += 1
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshPermissions()
        flowTick += 1
    }

    LaunchedEffect(pendingFlow, flowTick, includeLocation, notificationGranted, overlayGranted, locationGranted) {
        if (!pendingFlow) return@LaunchedEffect

        when {
            !notificationGranted && !notificationAttempted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                notificationAttempted = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            !notificationGranted -> {
                pendingFlow = false
                coroutineScope.launch { snackbarHostState.showSnackbar("알림 권한을 허용해주세요") }
            }

            !overlayGranted && !overlayAttempted -> {
                overlayAttempted = true
                overlaySettingsLauncher.launch(OverlayPermission.settingsIntent(context))
            }

            !overlayGranted -> {
                pendingFlow = false
                coroutineScope.launch { snackbarHostState.showSnackbar("화면 위 그리기를 허용해주세요") }
            }

            includeLocation && !locationGranted && !locationAttempted -> {
                locationAttempted = true
                locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }

            includeLocation && !locationGranted -> {
                pendingFlow = false
                coroutineScope.launch { snackbarHostState.showSnackbar("거리 위젯은 위치 권한이 필요해요") }
            }

            else -> {
                pendingFlow = false
                onReady()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = WarmCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCanvas)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header()
            PermissionCard(
                icon = Icons.Rounded.Notifications,
                title = "알림",
                body = "그리기 시작, 끄기, 전체 지우기를 알림에서 바로 조작해요.",
                granted = notificationGranted,
                required = true,
            )
            PermissionCard(
                icon = Icons.Rounded.Brush,
                title = "화면 위 그리기",
                body = "상대에게 낙서를 보내려면 지금 보는 화면 위에 선을 띄울 수 있어야 해요.",
                granted = overlayGranted,
                required = true,
            )
            PermissionCard(
                icon = Icons.Rounded.LocationOn,
                title = "위치",
                body = "거리 위젯을 직접 켤 때만 사용해요. 기본값은 꺼짐이에요.",
                granted = locationGranted,
                required = false,
                trailing = {
                    Switch(checked = includeLocation, onCheckedChange = { includeLocation = it })
                },
            )
            SafetyNoteCard()
            Spacer(Modifier.height(6.dp))
            RoundedPastelButton(
                text = if (pendingFlow) "확인 중..." else "권한 설정하기",
                enabled = !pendingFlow,
                onClick = {
                    refreshPermissions()
                    overlayAttempted = false
                    notificationAttempted = false
                    locationAttempted = false
                    pendingFlow = true
                    flowTick += 1
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryPastelButton(
                text = "다시 확인",
                enabled = !pendingFlow,
                onClick = {
                    refreshPermissions()
                    if (hasRequiredStartupPermissions(context)) {
                        onReady()
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar("필수 권한을 먼저 허용해주세요") }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun hasRequiredStartupPermissions(context: Context): Boolean =
    context.hasNotificationPermission() && OverlayPermission.canDrawOverlays(context)

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.hasLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
private fun Header() {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            BrandIconTile(Modifier.size(64.dp))
            Text(
                "화면 위에 마음을 보내려면",
                style = MaterialTheme.typography.headlineMedium,
                color = WarmBlack,
            )
            Text(
                "상대에게 낙서를 보내려면 알림과 화면 위 그리기 권한이 필요해요. lovedraw는 화면 내용을 읽거나 몰래 저장하지 않아요.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }
    }
}

@Composable
private fun SafetyNoteCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarmSurfaceAlt),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SunshineYellow.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Sand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = WarmBlack)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("권한은 이 기능에만 사용해요", style = MaterialTheme.typography.titleSmall, color = WarmBlack)
                Text("화면 캡처 없음 · 몰래 위치 추적 없음 · 언제든 설정에서 끌 수 있음", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    granted: Boolean,
    required: Boolean,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(if (granted) Mint.copy(alpha = 0.35f) else WarmSurfaceAlt, CircleShape)
                    .border(1.dp, if (granted) Mint else Sand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = WarmBlack)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = WarmBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (required) "필수" else "선택",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmBlack,
                        modifier = Modifier
                            .background(if (required) SunshineYellow else WarmSurfaceAlt, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(body, style = MaterialTheme.typography.bodyMedium, color = WarmGray)
            }
            if (trailing != null) {
                trailing()
            } else {
                PermissionStateDot(granted)
            }
        }
    }
}

@Composable
private fun PermissionStateDot(granted: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(if (granted) Mint else Color.Transparent, CircleShape)
            .border(1.dp, if (granted) Mint else Sand, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (granted) {
            Icon(Icons.Rounded.Check, contentDescription = "허용됨", tint = WarmBlack, modifier = Modifier.size(18.dp))
        }
    }
}
