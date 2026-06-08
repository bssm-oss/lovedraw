package com.example.couplecanvas

import com.example.couplecanvas.util.LegalLinksCardCopy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalLinksCardCopyTest {
    @Test
    fun legalCopyIsUserFacingAndMentionsDeletion() {
        val combined = listOf(
            LegalLinksCardCopy.TITLE,
            LegalLinksCardCopy.READY_BODY,
            LegalLinksCardCopy.NOT_READY_BODY,
            LegalLinksCardCopy.PRIVACY_BUTTON,
            LegalLinksCardCopy.DELETION_BUTTON,
            LegalLinksCardCopy.SUPPORT_BUTTON,
        ).joinToString(" ")

        assertTrue(combined.contains("개인정보"))
        assertTrue(combined.contains("삭제"))
        assertTrue(combined.contains("문의"))
    }

    @Test
    fun legalCopyDoesNotExposeDeveloperReleaseSetupLanguage() {
        val combined = LegalLinksCardCopy.READY_BODY + " " + LegalLinksCardCopy.NOT_READY_BODY

        assertFalse(combined.contains("출시 전"))
        assertFalse(combined.contains("운영자 연락처"))
        assertFalse(combined.contains("URL을 설정"))
        assertFalse(combined.contains("설정해야"))
    }
}
