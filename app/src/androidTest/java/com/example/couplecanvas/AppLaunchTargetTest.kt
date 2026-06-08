package com.example.couplecanvas

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couplecanvas.presentation.navigation.EXTRA_TARGET_ROOM_ID
import com.example.couplecanvas.presentation.navigation.EXTRA_TARGET_TAB_INDEX
import com.example.couplecanvas.presentation.navigation.toAppLaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchTargetTest {
    @Test
    fun parsesRoomAndTabFromWidgetIntentExtras() {
        val target = Intent()
            .putExtra(EXTRA_TARGET_ROOM_ID, "room-123")
            .putExtra(EXTRA_TARGET_TAB_INDEX, 3)
            .toAppLaunchTarget()

        requireNotNull(target)
        assertEquals("room-123", target.roomId)
        assertEquals(3, target.tabIndex)
        assertEquals("room/room-123?tab=3", target.route)
    }

    @Test
    fun ignoresMissingRoomAndClampsInvalidTab() {
        assertNull(Intent().putExtra(EXTRA_TARGET_TAB_INDEX, 3).toAppLaunchTarget())

        val target = Intent()
            .putExtra(EXTRA_TARGET_ROOM_ID, "room-123")
            .putExtra(EXTRA_TARGET_TAB_INDEX, 99)
            .toAppLaunchTarget()

        requireNotNull(target)
        assertEquals(7, target.tabIndex)
    }

    @Test
    fun parsesInviteDeepLink() {
        val target = Intent(Intent.ACTION_VIEW, Uri.parse("lovedraw://invite?code=abc123")).toAppLaunchTarget()

        requireNotNull(target)
        assertEquals("ABC123", target.inviteCode)
        assertEquals("home", target.route)
    }

    @Test
    fun ignoresInvalidInviteDeepLink() {
        assertNull(Intent(Intent.ACTION_VIEW, Uri.parse("lovedraw://invite?code=TOO-LONG")).toAppLaunchTarget())
        assertNull(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/invite?code=ABC123")).toAppLaunchTarget())
    }
}
