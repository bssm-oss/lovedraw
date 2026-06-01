package com.example.couplecanvas.presentation.navigation

import android.content.Intent

const val EXTRA_TARGET_ROOM_ID = "com.example.couplecanvas.extra.TARGET_ROOM_ID"
const val EXTRA_TARGET_TAB_INDEX = "com.example.couplecanvas.extra.TARGET_TAB_INDEX"

data class AppLaunchTarget(
    val roomId: String,
    val tabIndex: Int = 0,
) {
    val route: String = "room/$roomId?tab=$tabIndex"
}

fun Intent.toAppLaunchTarget(): AppLaunchTarget? {
    val roomId = getStringExtra(EXTRA_TARGET_ROOM_ID)?.takeIf { it.isNotBlank() } ?: return null
    return AppLaunchTarget(
        roomId = roomId,
        tabIndex = getIntExtra(EXTRA_TARGET_TAB_INDEX, 0).coerceIn(0, 7),
    )
}
