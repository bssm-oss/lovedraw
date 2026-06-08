package com.example.couplecanvas

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couplecanvas.util.InviteShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InviteShareHelperTest {
    @Test
    fun payloadContainsCodeLinkAndReadableText() {
        val payload = InviteShareHelper.payload("abC123", "우리 방")

        assertEquals("ABC123", payload.roomCode)
        assertEquals("lovedraw://invite?code=ABC123", payload.inviteLink)
        assertTrue(payload.inviteLink.startsWith("lovedraw://invite?code="))
        assertTrue(payload.inviteLink.none { it.isWhitespace() })
        assertTrue(payload.shareText.contains("우리 방"))
        assertTrue(payload.shareText.contains("ABC123"))
        assertTrue(payload.shareText.contains(payload.inviteLink))
    }

    @Test
    fun qrBitmapContainsReadableBlackAndWhiteModules() {
        val bitmap = InviteShareHelper.createQrBitmap("lovedraw://invite?code=ABC123", sizePx = 160)
        var black = 0
        var white = 0

        for (x in 0 until bitmap.width step 4) {
            for (y in 0 until bitmap.height step 4) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.red(pixel) < 80 && Color.green(pixel) < 80 && Color.blue(pixel) < 80) black++
                if (Color.red(pixel) > 230 && Color.green(pixel) > 230 && Color.blue(pixel) > 230) white++
            }
        }

        assertTrue("QR should contain dark modules.", black > 10)
        assertTrue("QR should contain light background modules.", white > 10)
    }
}
