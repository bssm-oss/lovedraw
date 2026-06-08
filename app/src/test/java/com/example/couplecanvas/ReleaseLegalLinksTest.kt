package com.example.couplecanvas

import com.example.couplecanvas.util.ReleaseLegalLinks
import com.example.couplecanvas.util.isConfiguredReleaseEmail
import com.example.couplecanvas.util.isConfiguredReleaseWebUrl
import com.example.couplecanvas.util.isValidEmail
import com.example.couplecanvas.util.isValidWebUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseLegalLinksTest {
    @Test
    fun validatesRequiredReleaseLegalLinks() {
        val ready = ReleaseLegalLinks(
            privacyPolicyUrl = "https://lovedraw.app/privacy",
            accountDeletionUrl = "https://lovedraw.app/delete-account",
            supportEmail = "support@lovedraw.app",
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
    fun rejectsPlaceholderReleaseLegalLinks() {
        val placeholder = ReleaseLegalLinks(
            privacyPolicyUrl = "https://your-domain.example/privacy",
            accountDeletionUrl = "https://example.com/delete-account",
            supportEmail = "support@example.com",
        )

        assertFalse(placeholder.hasPrivacyPolicyUrl)
        assertFalse(placeholder.hasAccountDeletionUrl)
        assertFalse(placeholder.hasSupportEmail)
        assertFalse(placeholder.isReleaseReady)
    }

    @Test
    fun validatesPrimitiveLinkFormats() {
        assertTrue("https://lovedraw.example/privacy".isValidWebUrl())
        assertTrue("hello@lovedraw.example".isValidEmail())
        assertFalse("ftp://lovedraw.example/privacy".isValidWebUrl())
        assertFalse("hello at lovedraw.example".isValidEmail())
    }

    @Test
    fun releaseLinksRequireHttpsAndNonPlaceholderValues() {
        assertTrue("https://lovedraw.app/privacy".isConfiguredReleaseWebUrl())
        assertTrue("support@lovedraw.app".isConfiguredReleaseEmail())
        assertFalse("http://lovedraw.app/privacy".isConfiguredReleaseWebUrl())
        assertFalse("https://your-domain.example/privacy".isConfiguredReleaseWebUrl())
        assertFalse("support@example.com".isConfiguredReleaseEmail())
    }
}
