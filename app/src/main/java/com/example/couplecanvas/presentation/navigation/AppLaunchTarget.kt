package com.example.couplecanvas.presentation.navigation

import android.content.Intent

const val EXTRA_TARGET_ROOM_ID = "com.example.couplecanvas.extra.TARGET_ROOM_ID"
const val EXTRA_TARGET_TAB_INDEX = "com.example.couplecanvas.extra.TARGET_TAB_INDEX"

data class AppLaunchTarget(
    val roomId: String? = null,
    val tabIndex: Int = 0,
    val inviteCode: String? = null,
) {
    val route: String = roomId?.let { "room/$it?tab=$tabIndex" } ?: "home"
}

fun Intent.toAppLaunchTarget(): AppLaunchTarget? {
    val roomId = getStringExtra(EXTRA_TARGET_ROOM_ID)?.takeIf { it.isNotBlank() }
    if (roomId != null) {
        return AppLaunchTarget(
            roomId = roomId,
            tabIndex = getIntExtra(EXTRA_TARGET_TAB_INDEX, 0).coerceIn(0, 7),
        )
    }

    val uri = data ?: return null
    if (uri.scheme != "lovedraw" || uri.host != "invite") return null
    val inviteCode = (uri.getQueryParameter("code") ?: uri.lastPathSegment)
        ?.trim()
        ?.uppercase()
        ?.takeIf { it.length == 6 }
        ?: return null
    return AppLaunchTarget(inviteCode = inviteCode)
}
