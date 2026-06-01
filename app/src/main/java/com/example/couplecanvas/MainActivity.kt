package com.example.couplecanvas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.couplecanvas.data.model.RoomHomeSummary
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.feature.overlay.CoupleOverlayService
import com.example.couplecanvas.feature.overlay.OverlayPermission
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.example.couplecanvas.presentation.navigation.AppLaunchTarget
import com.example.couplecanvas.presentation.navigation.CoupleCanvasNavHost
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.toAppLaunchTarget
import com.example.couplecanvas.presentation.theme.CoupleCanvasTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val launchTargetState = mutableStateOf<AppLaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchTargetState.value = intent?.toAppLaunchTarget()
        val app = application as CoupleCanvasApplication
        setContent {
            CompositionLocalProvider(LocalAppContainer provides app.container) {
                CoupleCanvasTheme {
                    CoupleCanvasNavHost(
                        launchTarget = launchTargetState.value,
                        onLaunchTargetConsumed = { launchTargetState.value = null },
                    )
                }
            }
        }
        startOverlayAutoStarter(app)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTargetState.value = intent.toAppLaunchTarget()
    }

    private fun hasOverlayNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun startOverlayAutoStarter(app: CoupleCanvasApplication) {
        lifecycleScope.launch {
            app.container.authRepository.observeUser().collectLatest { user ->
                if (user == null) return@collectLatest
                app.container.roomRepository.observeRoomSummariesForUser(user.uid).collectLatest { summaries ->
                    val summary = summaries.bestOverlayRoom() ?: return@collectLatest
                    if (!app.container.overlayStateStore.isEnabled()) return@collectLatest
                    if (!OverlayPermission.canDrawOverlays(this@MainActivity) || !hasOverlayNotificationPermission()) return@collectLatest

                    app.container.widgetStateStore.selectRoom(
                        roomId = summary.room.roomId,
                        roomTitle = summary.room.title,
                        privacyMode = summary.room.privacyMode,
                    )
                    startForegroundService(
                        CoupleOverlayService.showIntent(
                            context = this@MainActivity,
                            roomId = summary.room.roomId,
                            roomTitle = summary.room.title,
                            startDrawing = false,
                        ),
                    )
                }
            }
        }
    }

    private fun List<RoomHomeSummary>.bestOverlayRoom(): RoomHomeSummary? =
        filterNot { it.room.status == RoomStatus.Closed.value }
            .sortedWith(
                compareByDescending<RoomHomeSummary> { it.room.status == RoomStatus.Active.value }
                    .thenByDescending { it.room.activeUserCount }
                    .thenByDescending { it.room.updatedAt },
            )
            .firstOrNull()
}
