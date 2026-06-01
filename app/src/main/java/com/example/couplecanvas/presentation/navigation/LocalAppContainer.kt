package com.example.couplecanvas.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.couplecanvas.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
