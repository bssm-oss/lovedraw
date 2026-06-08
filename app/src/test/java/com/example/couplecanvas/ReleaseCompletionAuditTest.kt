package com.example.couplecanvas

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseCompletionAuditTest {
    @Test
    fun auditReferencesEveryReleaseGate() {
        val audit = projectFile("docs/release-completion-audit.md").readText()
        val requiredReferences = listOf(
            "PermissionOnboardingCopyTest",
            "OverlayQuickStartCopyTest",
            "RoomConnectionStatusTest",
            "StrokeInputInterpolatorTest",
            "StrokePointInterpolatorTest",
            "StrokeExpiryTest",
            "WaitingInviteCopyTest",
            "ShareVisibilityManifestTest",
            "ManifestPrivacyPolicyTest",
            "ReleaseLegalLinksTest",
            "ReleaseDocumentationConsistencyTest",
            "ReleaseEvidenceChecklistTest",
            "scripts/check_sensitive_files.sh",
            "scripts/verify_release_evidence.sh",
            "scripts/render_release_legal_docs.sh",
            ":app:verifyReleaseReadiness",
        )

        requiredReferences.forEach { reference ->
            assertTrue("Release audit must reference $reference.", audit.contains(reference))
        }
    }

    @Test
    fun auditDocumentsExternalReleaseBlockers() {
        val audit = projectFile("docs/release-completion-audit.md").readText()
        val blockers = listOf(
            "LOVEDRAW_PRIVACY_POLICY_URL",
            "LOVEDRAW_ACCOUNT_DELETION_URL",
            "LOVEDRAW_SUPPORT_EMAIL",
            "LOVEDRAW_OPERATOR_NAME",
            "LOVEDRAW_POLICY_EFFECTIVE_DATE",
            "LOVEDRAW_DELETION_PROCESSING_PERIOD",
            "COUPLE_CANVAS_DATABASE_URL",
            "app/google-services.json",
            "LOVEDRAW_RELEASE_STORE_FILE",
            "release SHA-1/SHA-256",
            "Google Play Console",
            "실제 Android 기기 2대",
        )

        assertTrue(audit.contains("코드에서 완료한 항목"))
        assertTrue(audit.contains("외부 입력이 필요한 항목"))
        blockers.forEach { blocker ->
            assertTrue("Release audit must call out external blocker $blocker.", audit.contains(blocker))
        }
    }

    @Test
    fun auditKeepsPrivateEvidenceOutOfGit() {
        val audit = projectFile("docs/release-completion-audit.md").readText()

        assertTrue(audit.contains("release-evidence/"))
        assertTrue(audit.contains("LOVEDRAW_RELEASE_EVIDENCE_DIR"))
        assertTrue(audit.contains("저장소에 커밋하지 않습니다"))
    }

    private fun projectFile(path: String): File =
        listOf(
            File(path),
            File("../$path"),
        ).first { it.isFile }
}
