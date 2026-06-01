package com.example.couplecanvas.feature.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.couplecanvas.MainActivity
import com.example.couplecanvas.data.local.WidgetStateStore
import com.example.couplecanvas.data.model.WidgetSnapshot
import com.example.couplecanvas.presentation.navigation.EXTRA_TARGET_ROOM_ID
import com.example.couplecanvas.presentation.navigation.EXTRA_TARGET_TAB_INDEX
import androidx.glance.GlanceModifier
import androidx.glance.GlanceId
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first

private val bg = ColorProvider(Color(0xFFF6F4EF))
private val ink = ColorProvider(Color(0xFF2A2926))
private val muted = ColorProvider(Color(0xFF706B63))
private val accent = ColorProvider(Color(0xFFE3AA00))

private object WidgetTab {
    const val Drawing = 0
    const val Notes = 1
    const val DatePlanner = 2
    const val Memories = 3
    const val DailySpark = 5
    const val Stats = 7
}

@Composable
private fun WidgetCard(
    context: Context,
    title: String,
    body: String,
    privacyMode: Boolean = false,
    roomId: String = "",
    targetTab: Int = WidgetTab.Stats,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .clickable(openAppAction(context, roomId, targetTab))
            .padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(title, style = TextStyle(color = accent, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        Text(if (privacyMode) privacyBody(title) else body, modifier = GlanceModifier.fillMaxWidth(), style = TextStyle(color = ink, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(4.dp))
        Text("Couple Canvas", style = TextStyle(color = muted))
    }
}

@Composable
private fun DrawingWidgetCard(context: Context, snapshot: WidgetSnapshot, bitmap: Bitmap?) {
    if (snapshot.privacyMode || bitmap == null) {
        WidgetCard(
            context = context,
            title = "최근 낙서",
            body = snapshot.latestDrawingText,
            privacyMode = snapshot.privacyMode,
            roomId = snapshot.roomId,
            targetTab = WidgetTab.Drawing,
        )
        return
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .clickable(openAppAction(context, snapshot.roomId, WidgetTab.Drawing))
            .padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text("최근 낙서", style = TextStyle(color = accent, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "최근 저장한 낙서",
            modifier = GlanceModifier.fillMaxWidth().height(96.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(snapshot.roomTitle.ifBlank { "Couple Canvas" }, style = TextStyle(color = muted))
    }
}

@Composable
private fun MemoryWidgetCard(context: Context, snapshot: WidgetSnapshot, bitmap: Bitmap?) {
    if (snapshot.privacyMode || bitmap == null) {
        WidgetCard(
            context = context,
            title = "최근 추억",
            body = snapshot.latestMemoryImageUrl?.let { "추억 있음" } ?: snapshot.latestMemoryTitle,
            privacyMode = snapshot.privacyMode,
            roomId = snapshot.roomId,
            targetTab = WidgetTab.Memories,
        )
        return
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .clickable(openAppAction(context, snapshot.roomId, WidgetTab.Memories))
            .padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text("최근 추억", style = TextStyle(color = accent, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "최근 추억 사진",
            modifier = GlanceModifier.fillMaxWidth().height(96.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(snapshot.latestMemoryTitle, style = TextStyle(color = ink, fontWeight = FontWeight.Medium))
    }
}

private fun openAppAction(context: Context, roomId: String = "", targetTab: Int = WidgetTab.Stats) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (roomId.isNotBlank()) {
            putExtra(EXTRA_TARGET_ROOM_ID, roomId)
            putExtra(EXTRA_TARGET_TAB_INDEX, targetTab)
        }
    }
)

private fun privacyBody(title: String): String =
    when (title) {
        "러브노트" -> "새 노트가 있어요"
        "다음 데이트" -> "앱에서 다음 데이트를 확인해요"
        "최근 추억" -> "앱에서 추억을 확인해요"
        "최근 낙서" -> "앱에서 낙서를 확인해요"
        "거리" -> "앱에서 거리 상태를 확인해요"
        else -> "앱에서 확인하기"
    }

private suspend fun latestSnapshot(context: Context): WidgetSnapshot =
    WidgetStateStore(context).snapshot.first()

class DaysTogetherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        provideContent { WidgetCard(context, "함께한 날", snapshot.daysTogetherText, roomId = snapshot.roomId, targetTab = WidgetTab.Stats) }
    }
}

class DateCountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        provideContent { WidgetCard(context, "다음 데이트", snapshot.nextDateText, snapshot.privacyMode, snapshot.roomId, WidgetTab.DatePlanner) }
    }
}

class DrawingPreviewWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        val bitmap = snapshot.latestDrawingLocalPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
        provideContent { DrawingWidgetCard(context, snapshot, bitmap) }
    }
}

class MemoryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        val bitmap = snapshot.latestMemoryLocalPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
        provideContent { MemoryWidgetCard(context, snapshot, bitmap) }
    }
}

class LoveNoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        provideContent { WidgetCard(context, "러브노트", snapshot.latestNoteText, snapshot.privacyMode, snapshot.roomId, WidgetTab.Notes) }
    }
}

class DailySparkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        provideContent { WidgetCard(context, "Daily Spark", snapshot.dailySparkText, roomId = snapshot.roomId, targetTab = WidgetTab.DailySpark) }
    }
}

class StatsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        provideContent { WidgetCard(context, "우리 통계", snapshot.statsText, roomId = snapshot.roomId, targetTab = WidgetTab.Stats) }
    }
}

class DistanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = latestSnapshot(context)
        provideContent { WidgetCard(context, "거리", snapshot.distanceText, snapshot.privacyMode, snapshot.roomId, WidgetTab.Stats) }
    }
}

suspend fun updateCoupleWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    suspend fun <T : GlanceAppWidget> update(widget: T, clazz: Class<T>) {
        manager.getGlanceIds(clazz).forEach { widget.update(context, it) }
    }
    update(DaysTogetherWidget(), DaysTogetherWidget::class.java)
    update(DateCountdownWidget(), DateCountdownWidget::class.java)
    update(DrawingPreviewWidget(), DrawingPreviewWidget::class.java)
    update(MemoryWidget(), MemoryWidget::class.java)
    update(LoveNoteWidget(), LoveNoteWidget::class.java)
    update(DailySparkWidget(), DailySparkWidget::class.java)
    update(StatsWidget(), StatsWidget::class.java)
    update(DistanceWidget(), DistanceWidget::class.java)
}

class DaysTogetherWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DaysTogetherWidget() }
class DateCountdownWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DateCountdownWidget() }
class DrawingPreviewWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DrawingPreviewWidget() }
class MemoryWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = MemoryWidget() }
class LoveNoteWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = LoveNoteWidget() }
class DailySparkWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DailySparkWidget() }
class StatsWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = StatsWidget() }
class DistanceWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DistanceWidget() }
