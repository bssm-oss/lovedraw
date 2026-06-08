package com.example.couplecanvas

import com.example.couplecanvas.util.LoginLegalConsentCopy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginLegalConsentCopyTest {
    @Test
    fun consentCopyMentionsPrivacyAndDeletionBeforeLogin() {
        val combined = listOf(
            LoginLegalConsentCopy.TITLE,
            LoginLegalConsentCopy.BODY,
            LoginLegalConsentCopy.CHECKBOX,
            LoginLegalConsentCopy.REQUIRED,
            LoginLegalConsentCopy.LINKS_REQUIRED,
        ).joinToString(" ")

        assertTrue(combined.contains("Google 계정"))
        assertTrue(combined.contains("개인정보처리방침"))
        assertTrue(combined.contains("계정/데이터 삭제"))
        assertTrue(combined.contains("확인"))
        assertTrue(combined.contains("삭제 요청 경로"))
    }

    @Test
    fun consentCopyDoesNotOverpromiseOrUseTrackingLanguage() {
        val combined = LoginLegalConsentCopy.BODY + " " + LoginLegalConsentCopy.CHECKBOX

        assertTrue(combined.contains("기록 복구"))
        assertTrue(combined.contains("방 동기화"))
        assertFalse(combined.contains("광고"))
        assertFalse(combined.contains("추적"))
        assertFalse(combined.contains("몰래"))
    }
}
