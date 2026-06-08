package com.example.couplecanvas

import com.example.couplecanvas.util.PermissionOnboardingCopy
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseDocumentationConsistencyTest {
    @Test
    fun foregroundServiceSubmissionCopyMatchesInAppPermissionExplanation() {
        val foregroundDoc = doc("docs/foreground-service-special-use-ko.md").readText()

        assertTrue(foregroundDoc.contains(PermissionOnboardingCopy.HEADER_BODY))
        assertTrue(foregroundDoc.contains(PermissionOnboardingCopy.OVERLAY_BODY))
        assertTrue(foregroundDoc.contains("화면 내용을 캡처하지 않음"))
        assertTrue(foregroundDoc.contains("사용자가 언제든 끌 수 있음"))
    }

    @Test
    fun dataSafetyDocIncludesReleaseDeletionMetadataKeys() {
        val dataSafetyDoc = doc("docs/play-console-data-safety-ko.md").readText()

        assertTrue(dataSafetyDoc.contains("LOVEDRAW_ACCOUNT_DELETION_URL"))
        assertTrue(dataSafetyDoc.contains("LOVEDRAW_SUPPORT_EMAIL"))
        assertTrue(dataSafetyDoc.contains("LOVEDRAW_DELETION_PROCESSING_PERIOD"))
        assertTrue(dataSafetyDoc.contains("광고 목적 데이터 사용: 아니오"))
        assertTrue(dataSafetyDoc.contains("분석 SDK 사용: 아니오"))
    }

    @Test
    fun renderScriptExportsAllReleaseSubmissionDocuments() {
        val script = doc("scripts/render_release_legal_docs.sh").readText()

        assertTrue(script.contains("privacy-policy-ko.md"))
        assertTrue(script.contains("account-data-deletion-page-ko.md"))
        assertTrue(script.contains("play-console-data-safety-ko.md"))
        assertTrue(script.contains("foreground-service-special-use-ko.md"))
    }

    private fun doc(path: String): File =
        listOf(
            File(path),
            File("../$path"),
        ).first { it.isFile }
}
