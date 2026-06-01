package com.example.couplecanvas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.couplecanvas.feature.overlay.CoupleOverlayService
import com.example.couplecanvas.feature.overlay.OverlayPermission
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.example.couplecanvas.presentation.navigation.AppLaunchTarget
import com.example.couplecanvas.presentation.navigation.CoupleCanvasNavHost
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.toAppLaunchTarget
import com.example.couplecanvas.presentation.theme.CoupleCanvasTheme
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
        lifecycleScope.launch {
            if (
                app.container.overlayStateStore.isEnabled() &&
                OverlayPermission.canDrawOverlays(this@MainActivity) &&
                hasOverlayNotificationPermission()
            ) {
                startForegroundService(CoupleOverlayService.showIntent(this@MainActivity))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTargetState.value = intent.toAppLaunchTarget()
    }

    private fun hasOverlayNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
