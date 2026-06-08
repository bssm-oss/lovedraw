package com.example.couplecanvas

import com.example.couplecanvas.util.WaitingInviteCopy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingInviteCopyTest {
    @Test
    fun waitingInviteCopyMentionsEveryShareOption() {
        val copy = WaitingInviteCopy.PRIMARY_BUTTON + " " + WaitingInviteCopy.HELPER

        assertTrue(copy.contains("카톡"))
        assertTrue(copy.contains("인스타 DM"))
        assertTrue(copy.contains("QR"))
        assertTrue(copy.contains("링크"))
    }

    @Test
    fun waitingInviteCopyStaysUserFacing() {
        val copy = WaitingInviteCopy.PRIMARY_BUTTON + " " + WaitingInviteCopy.HELPER

        assertFalse(copy.contains("Firebase"))
        assertFalse(copy.contains("RTDB"))
        assertFalse(copy.contains("Intent"))
    }
}
