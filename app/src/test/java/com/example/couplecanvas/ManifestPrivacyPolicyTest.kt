package com.example.couplecanvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ManifestPrivacyPolicyTest {
    @Test
    fun manifestRequestsOnlyReviewedRuntimeAndNetworkPermissions() {
        val permissions = manifestDocument()
            .getElementsByTagName("uses-permission")
            .asSequence()
            .map { it.attribute("android:name") }
            .toSet()

        assertEquals(
            "New manifest permissions must be reviewed against privacy policy and Play Console Data safety before release.",
            setOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
            ),
            permissions,
        )
    }

    @Test
    fun manifestDoesNotDeclareHiddenTrackingOrCaptureSurfaces() {
        val manifestText = androidManifest().readText()
        val forbidden = listOf(
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.accessibilityservice.AccessibilityService",
        )

        forbidden.forEach { value ->
            assertFalse("Manifest must not declare $value.", manifestText.contains(value))
        }
    }

    @Test
    fun manifestDisablesAndroidBackupForPrivateCoupleData() {
        val application = manifestDocument()
            .getElementsByTagName("application")
            .asSequence()
            .single()

        assertEquals("false", application.attribute("android:allowBackup"))
        assertTrue(application.attribute("android:dataExtractionRules").isNotBlank())
        assertTrue(application.attribute("android:fullBackupContent").isNotBlank())
    }

    private fun manifestDocument() =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(androidManifest())

    private fun androidManifest(): File =
        listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).first { it.isFile }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Element> =
        (0 until length).asSequence().map { item(it) as Element }

    private fun Element.attribute(name: String): String =
        attributes.getNamedItem(name)?.nodeValue.orEmpty()
}
