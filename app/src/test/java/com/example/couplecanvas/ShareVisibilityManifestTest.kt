package com.example.couplecanvas

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ShareVisibilityManifestTest {
    @Test
    fun manifestDeclaresTargetedSharePackageVisibility() {
        val manifest = androidManifest()
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val packages = document.getElementsByTagName("package").asSequence()
            .map { it.attributes.getNamedItem("android:name")?.nodeValue.orEmpty() }
            .toSet()

        assertTrue("KakaoTalk package visibility is required for targeted invite sharing.", "com.kakao.talk" in packages)
        assertTrue("Instagram package visibility is required for targeted invite sharing.", "com.instagram.android" in packages)
    }

    @Test
    fun manifestDeclaresGenericShareAndSupportEmailVisibility() {
        val manifestText = androidManifest().readText()

        assertTrue(manifestText.contains("android.intent.action.SEND"))
        assertTrue(manifestText.contains("android:mimeType=\"text/plain\""))
        assertTrue(manifestText.contains("android.intent.action.SENDTO"))
        assertTrue(manifestText.contains("android:scheme=\"mailto\""))
    }

    private fun androidManifest(): File =
        listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).first { it.isFile }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> =
        (0 until length).asSequence().map { item(it) }
}
