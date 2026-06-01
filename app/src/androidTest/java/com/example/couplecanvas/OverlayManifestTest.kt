package com.example.couplecanvas

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.couplecanvas.feature.overlay.CoupleOverlayService
import com.example.couplecanvas.feature.overlay.OverlayPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayManifestTest {
    @Test
    fun declaresOverlayAndForegroundServicePermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.packageInfoWithPermissions(context.packageName)
        val permissions = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(Manifest.permission.SYSTEM_ALERT_WINDOW in permissions)
        assertTrue(Manifest.permission.FOREGROUND_SERVICE in permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assertTrue(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE in permissions)
        }
    }

    @Test
    fun overlayServiceIsPrivateSpecialUseForegroundService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.packageInfoWithServices(context.packageName)
        val service = packageInfo.services.orEmpty()
            .single { it.name == CoupleOverlayService::class.java.name }

        assertFalse(service.exported)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assertTrue(service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0)
            val property = context.packageManager.getProperty(
                PackageManager.PROPERTY_SPECIAL_USE_FGS_SUBTYPE,
                ComponentName(context, CoupleOverlayService::class.java),
            )
            assertEquals(
                "User-enabled Couple Canvas drawing overlay shown over other apps so the user can draw directly on the screen and sync strokes with their selected room.",
                property.string,
            )
        }
    }

    @Test
    fun overlayPermissionSettingsIntentTargetsThisPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = OverlayPermission.settingsIntent(context)

        assertEquals("android.settings.action.MANAGE_OVERLAY_PERMISSION", intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.packageInfoWithPermissions(packageName: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }

    @Suppress("DEPRECATION")
    private fun PackageManager.packageInfoWithServices(packageName: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of((PackageManager.GET_SERVICES or PackageManager.GET_META_DATA).toLong()),
            )
        } else {
            getPackageInfo(packageName, PackageManager.GET_SERVICES or PackageManager.GET_META_DATA)
        }
}
