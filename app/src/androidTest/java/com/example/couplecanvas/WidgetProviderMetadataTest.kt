package com.example.couplecanvas

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetProviderMetadataTest {
    @Test
    fun allWidgetsDeclareHomeKeyguardResizeAndDescriptionMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = listOf(
            WidgetProvider(".feature.widgets.DaysTogetherWidgetReceiver", expectedTargetHeight = 1),
            WidgetProvider(".feature.widgets.DateCountdownWidgetReceiver", expectedTargetHeight = 1),
            WidgetProvider(".feature.widgets.DrawingPreviewWidgetReceiver", expectedTargetHeight = 2),
            WidgetProvider(".feature.widgets.MemoryWidgetReceiver", expectedTargetHeight = 2),
            WidgetProvider(".feature.widgets.LoveNoteWidgetReceiver", expectedTargetHeight = 1),
            WidgetProvider(".feature.widgets.DailySparkWidgetReceiver", expectedTargetHeight = 1),
            WidgetProvider(".feature.widgets.StatsWidgetReceiver", expectedTargetHeight = 2),
            WidgetProvider(".feature.widgets.DistanceWidgetReceiver", expectedTargetHeight = 1),
        )
        val installedProviders = AppWidgetManager.getInstance(context)
            .installedProviders
            .filter { it.provider.packageName == context.packageName }
            .associateBy { it.provider.className }

        providers.forEach { provider ->
            val className = context.packageName + provider.receiverSuffix
            val info = installedProviders[className]

            assertNotNull("Widget provider $className must be registered.", info)
            checkNotNull(info)

            assertTrue("Widget $className must have a user-facing description.", info.descriptionRes != 0)
            assertTrue(context.getString(info.descriptionRes).isNotBlank())
            assertTrue(
                "Widget $className must be available for home screen.",
                info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0,
            )
            assertTrue(
                "Widget $className must opt into supported keyguard surfaces.",
                info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD != 0,
            )
            assertTrue(
                "Widget $className must resize horizontally.",
                info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0,
            )
            assertTrue(
                "Widget $className must resize vertically.",
                info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0,
            )
            assertTrue("Widget $className must declare minResizeWidth.", info.minResizeWidth > 0)
            assertTrue("Widget $className must declare minResizeHeight.", info.minResizeHeight > 0)
            assertEquals(R.layout.glance_default_loading, info.previewLayout)
            assertEquals(R.layout.glance_default_loading, info.initialLayout)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertEquals(3, info.targetCellWidth)
                assertEquals(provider.expectedTargetHeight, info.targetCellHeight)
            }
        }
    }

    private data class WidgetProvider(
        val receiverSuffix: String,
        val expectedTargetHeight: Int,
    )
}
