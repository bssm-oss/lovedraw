package com.example.couplecanvas.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.couplecanvas.data.model.WidgetSnapshot
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.widgetDataStore by preferencesDataStore("widget_state")

class WidgetStateStore(private val context: Context) {
    private object Keys {
        val roomId = stringPreferencesKey("room_id")
        val roomTitle = stringPreferencesKey("room_title")
        val privacyMode = booleanPreferencesKey("privacy_mode")
        val days = stringPreferencesKey("days")
        val nextDate = stringPreferencesKey("next_date")
        val latestNote = stringPreferencesKey("latest_note")
        val spark = stringPreferencesKey("spark")
        val memory = stringPreferencesKey("memory")
        val memoryId = stringPreferencesKey("memory_id")
        val memoryImage = stringPreferencesKey("memory_image")
        val memoryLocalPath = stringPreferencesKey("memory_local_path")
        val drawing = stringPreferencesKey("drawing")
        val drawingLocalPath = stringPreferencesKey("drawing_local_path")
        val stats = stringPreferencesKey("stats")
        val distance = stringPreferencesKey("distance")
        val updatedAt = longPreferencesKey("updated_at")
    }

    val snapshot: Flow<WidgetSnapshot> = context.widgetDataStore.data.map { prefs ->
        WidgetSnapshot(
            roomId = prefs[Keys.roomId].orEmpty(),
            roomTitle = prefs[Keys.roomTitle].orEmpty(),
            privacyMode = prefs[Keys.privacyMode] ?: false,
            daysTogetherText = prefs[Keys.days] ?: "함께한 날을 설정해요",
            nextDateText = prefs[Keys.nextDate] ?: "다음 데이트를 정해볼까요?",
            latestNoteText = prefs[Keys.latestNote] ?: "새 노트가 있으면 여기에 보여요",
            dailySparkText = prefs[Keys.spark] ?: "오늘의 질문을 열어보세요",
            latestMemoryTitle = prefs[Keys.memory] ?: "추억을 추가해보세요",
            latestMemoryId = prefs[Keys.memoryId],
            latestMemoryImageUrl = prefs[Keys.memoryImage],
            latestMemoryLocalPath = prefs[Keys.memoryLocalPath],
            latestDrawingUrl = prefs[Keys.drawing],
            latestDrawingLocalPath = prefs[Keys.drawingLocalPath],
            statsText = prefs[Keys.stats] ?: "아직 통계가 없어요",
            distanceText = prefs[Keys.distance] ?: "위치 공유가 꺼져 있어요",
            updatedAt = prefs[Keys.updatedAt] ?: 0L,
        )
    }

    suspend fun currentRoomId(): String =
        context.widgetDataStore.data.map { prefs -> prefs[Keys.roomId].orEmpty() }.first()

    suspend fun shouldUpdateRoom(roomId: String): Boolean {
        val currentRoomId = currentRoomId()
        return currentRoomId.isBlank() || currentRoomId == roomId
    }

    suspend fun selectRoom(roomId: String, roomTitle: String, privacyMode: Boolean = false) {
        context.widgetDataStore.edit { prefs ->
            prefs[Keys.roomId] = roomId
            prefs[Keys.roomTitle] = roomTitle
            prefs[Keys.privacyMode] = privacyMode
            prefs[Keys.updatedAt] = System.currentTimeMillis()
        }
    }

    suspend fun save(snapshot: WidgetSnapshot, afterSave: suspend (Context) -> Unit = {}) {
        context.widgetDataStore.edit { prefs ->
            val previousRoomId = prefs[Keys.roomId]
            val previousMemoryId = prefs[Keys.memoryId]
            val previousMemoryImage = prefs[Keys.memoryImage]
            prefs[Keys.roomId] = snapshot.roomId
            prefs[Keys.roomTitle] = snapshot.roomTitle
            prefs[Keys.privacyMode] = snapshot.privacyMode
            prefs[Keys.days] = snapshot.daysTogetherText
            prefs[Keys.nextDate] = if (snapshot.privacyMode) "앱에서 다음 데이트를 확인해요" else snapshot.nextDateText
            prefs[Keys.latestNote] = if (snapshot.privacyMode) "새 노트가 있어요" else snapshot.latestNoteText
            prefs[Keys.spark] = if (snapshot.privacyMode) "오늘의 질문이 도착했어요" else snapshot.dailySparkText
            prefs[Keys.memory] = if (snapshot.privacyMode) "앱에서 추억을 확인해요" else snapshot.latestMemoryTitle
            if (snapshot.latestMemoryId == null) {
                prefs.remove(Keys.memoryId)
                prefs.remove(Keys.memoryImage)
                prefs.remove(Keys.memoryLocalPath)
            } else {
                prefs[Keys.memoryId] = snapshot.latestMemoryId
                if (snapshot.latestMemoryImageUrl == null) {
                    prefs.remove(Keys.memoryImage)
                } else {
                    prefs[Keys.memoryImage] = snapshot.latestMemoryImageUrl
                }
                if (snapshot.latestMemoryLocalPath != null) {
                    prefs[Keys.memoryLocalPath] = snapshot.latestMemoryLocalPath
                } else if (
                    previousRoomId != snapshot.roomId ||
                    previousMemoryId != snapshot.latestMemoryId ||
                    previousMemoryImage != snapshot.latestMemoryImageUrl
                ) {
                    prefs.remove(Keys.memoryLocalPath)
                }
            }
            if (snapshot.latestDrawingUrl == null) {
                if (previousRoomId != snapshot.roomId) prefs.remove(Keys.drawing)
            } else {
                prefs[Keys.drawing] = snapshot.latestDrawingUrl
            }
            if (snapshot.latestDrawingLocalPath == null) {
                if (previousRoomId != snapshot.roomId) prefs.remove(Keys.drawingLocalPath)
            } else {
                prefs[Keys.drawingLocalPath] = snapshot.latestDrawingLocalPath
            }
            prefs[Keys.stats] = snapshot.statsText
            prefs[Keys.distance] = snapshot.distanceText
            prefs[Keys.updatedAt] = snapshot.updatedAt
        }
        afterSave(context)
    }

    suspend fun updateLatestMemory(
        roomId: String,
        title: String,
        memoryId: String,
        latestMemoryImageUrl: String?,
        latestMemoryLocalPath: String? = null,
        afterSave: suspend (Context) -> Unit = {},
    ) {
        context.widgetDataStore.edit { prefs ->
            val previousMemoryId = prefs[Keys.memoryId]
            val previousMemoryImage = prefs[Keys.memoryImage]
            val privacyMode = prefs[Keys.privacyMode] ?: false
            prefs[Keys.roomId] = roomId
            prefs[Keys.memory] = if (privacyMode) "앱에서 추억을 확인해요" else title
            prefs[Keys.memoryId] = memoryId
            if (latestMemoryImageUrl == null) {
                prefs.remove(Keys.memoryImage)
            } else {
                prefs[Keys.memoryImage] = latestMemoryImageUrl
            }
            if (latestMemoryLocalPath != null) {
                prefs[Keys.memoryLocalPath] = latestMemoryLocalPath
            } else if (previousMemoryId != memoryId || previousMemoryImage != latestMemoryImageUrl) {
                prefs.remove(Keys.memoryLocalPath)
            }
            prefs[Keys.updatedAt] = System.currentTimeMillis()
        }
        afterSave(context)
    }

    suspend fun updateLatestDrawing(
        roomId: String,
        latestDrawingUrl: String,
        latestDrawingLocalPath: String? = null,
        afterSave: suspend (Context) -> Unit = {},
    ) {
        context.widgetDataStore.edit { prefs ->
            prefs[Keys.roomId] = roomId
            prefs[Keys.drawing] = latestDrawingUrl
            if (latestDrawingLocalPath != null) {
                prefs[Keys.drawingLocalPath] = latestDrawingLocalPath
            }
            prefs[Keys.updatedAt] = System.currentTimeMillis()
        }
        afterSave(context)
    }

    suspend fun cacheLatestDrawingPreviewFromUrl(roomId: String, imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        val current = snapshot.first()
        current.latestDrawingLocalPath
            ?.takeIf { current.roomId == roomId && current.latestDrawingUrl == imageUrl && File(it).exists() }
            ?.let { return it }

        return withContext(Dispatchers.IO) {
            downloadBitmap(imageUrl)?.let { bitmap ->
                try {
                    saveLatestDrawingPreview(roomId, bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    suspend fun cacheLatestMemoryPreviewFromUrl(roomId: String, memoryId: String, imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        val current = snapshot.first()
        current.latestMemoryLocalPath
            ?.takeIf {
                current.roomId == roomId &&
                    current.latestMemoryId == memoryId &&
                    current.latestMemoryImageUrl == imageUrl &&
                    File(it).exists()
            }
            ?.let { return it }

        return withContext(Dispatchers.IO) {
            downloadBitmap(imageUrl)?.let { bitmap ->
                try {
                    saveLatestMemoryPreview(roomId, memoryId, bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    fun saveLatestDrawingPreview(roomId: String, bitmap: Bitmap): String {
        val safeRoomId = roomId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory = File(context.filesDir, "widget_previews").apply { mkdirs() }
        val file = File(directory, "latest_drawing_$safeRoomId.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 96, stream)
        }
        return file.absolutePath
    }

    fun saveLatestMemoryPreview(roomId: String, memoryId: String, uri: Uri): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("위젯에 표시할 추억 사진을 열지 못했어요")
        return saveLatestMemoryPreview(roomId, memoryId, bitmap)
    }

    fun saveLatestMemoryPreview(roomId: String, memoryId: String, bitmap: Bitmap): String {
        val safeRoomId = roomId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val safeMemoryId = memoryId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory = File(context.filesDir, "widget_previews").apply { mkdirs() }
        val file = File(directory, "latest_memory_${safeRoomId}_$safeMemoryId.jpg")
        val preview = bitmap.scaledForWidgetPreview()
        FileOutputStream(file).use { stream ->
            preview.compress(Bitmap.CompressFormat.JPEG, 86, stream)
        }
        if (preview !== bitmap) preview.recycle()
        return file.absolutePath
    }

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        val connection = URL(imageUrl).openConnection().apply {
            connectTimeout = 5_000
            readTimeout = 8_000
        }
        return try {
            if (connection is HttpURLConnection) {
                connection.instanceFollowRedirects = true
                connection.connect()
                if (connection.responseCode !in 200..299) return null
            }
            connection.getInputStream().use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        } finally {
            if (connection is HttpURLConnection) connection.disconnect()
        }
    }

    private fun Bitmap.scaledForWidgetPreview(maxEdge: Int = 900): Bitmap {
        val largest = maxOf(width, height)
        if (largest <= maxEdge) return this
        val scale = maxEdge.toFloat() / largest.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
