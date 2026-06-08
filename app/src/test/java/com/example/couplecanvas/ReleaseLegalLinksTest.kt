package com.example.couplecanvas

import com.example.couplecanvas.util.ReleaseLegalLinks
import com.example.couplecanvas.util.isValidEmail
import com.example.couplecanvas.util.isValidWebUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseLegalLinksTest {
    @Test
    fun validatesRequiredReleaseLegalLinks() {
        val ready = ReleaseLegalLinks(
            privacyPolicyUrl = "https://example.com/privacy",
            accountDeletionUrl = "https://example.com/delete-account",
            supportEmail = "support@example.com",
        )

        assertTrue(ready.hasPrivacyPolicyUrl)
        assertTrue(ready.hasAccountDeletionUrl)
        assertTrue(ready.hasSupportEmail)
        assertTrue(ready.isReleaseReady)
    }

    @Test
    fun rejectsMissingReleaseLegalLinks() {
        val missing = ReleaseLegalLinks(
            privacyPolicyUrl = "",
            accountDeletionUrl = "not a url",
            supportEmail = "support",
        )

        assertFalse(missing.hasPrivacyPolicyUrl)
        assertFalse(missing.hasAccountDeletionUrl)
        assertFalse(missing.hasSupportEmail)
        assertFalse(missing.isReleaseReady)
    }

    @Test
    fun validatesPrimitiveLinkFormats() {
        assertTrue("https://lovedraw.example/privacy".isValidWebUrl())
        assertTrue("hello@lovedraw.example".isValidEmail())
        assertFalse("ftp://lovedraw.example/privacy".isValidWebUrl())
        assertFalse("hello at lovedraw.example".isValidEmail())
    }
}
