package com.example.couplecanvas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.example.couplecanvas.presentation.navigation.AppLaunchTarget
import com.example.couplecanvas.presentation.navigation.CoupleCanvasNavHost
import com.example.couplecanvas.presentation.navigation.LocalAppContainer
import com.example.couplecanvas.presentation.navigation.toAppLaunchTarget
import com.example.couplecanvas.presentation.theme.CoupleCanvasTheme

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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTargetState.value = intent.toAppLaunchTarget()
    }
}
