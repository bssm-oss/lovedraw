package com.example.couplecanvas

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseEvidenceChecklistTest {
    @Test
    fun checklistDocumentsEveryVerifierArtifactSlug() {
        val verifier = projectFile("scripts/verify_release_evidence.sh").readText()
        val checklist = projectFile("docs/release-evidence-checklist.md").readText()
        val slugs = Regex("\"([a-z0-9-]+)\\|")
            .findAll(verifier)
            .map { it.groupValues[1] }
            .toSet()

        assertTrue("Verifier must declare at least one artifact slug.", slugs.isNotEmpty())
        assertTrue(
            "Verifier must print an example filename for missing artifacts.",
            verifier.contains("Example filename: \$slug.mp4 or \$slug.png"),
        )
        slugs.forEach { slug ->
            assertTrue("Checklist must document `$slug`.", checklist.contains("`$slug`"))
        }
    }

    @Test
    fun checklistMakesEvidenceLocalOnly() {
        val checklist = projectFile("docs/release-evidence-checklist.md").readText()

        assertTrue(checklist.contains("release-evidence/"))
        assertTrue(checklist.contains("LOVEDRAW_RELEASE_EVIDENCE_DIR"))
        assertTrue(checklist.contains("저장소에 커밋하지 않습니다"))
        assertTrue(checklist.contains("사람이 직접 확인"))
    }

    private fun projectFile(path: String): File =
        listOf(
            File(path),
            File("../$path"),
        ).first { it.isFile }
}
