package com.example.couplecanvas

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.couplecanvas.data.local.WidgetStateStore
import com.example.couplecanvas.data.model.WidgetSnapshot
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetStateStoreIntegrationTest {
    @Test
    fun privacyModeHidesSensitiveWidgetTextAndPreservesSameRoomDrawingPreview() {
        runBlocking {
            withTimeout(10_000) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val store = WidgetStateStore(context)
                val previewPath = store.saveLatestDrawingPreview(
                    roomId = "room-widget-test",
                    bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888),
                )
                val memoryPreviewPath = store.saveLatestMemoryPreview(
                    roomId = "room-widget-test",
                    memoryId = "memory-widget-test",
                    bitmap = Bitmap.createBitmap(6, 6, Bitmap.Config.ARGB_8888),
                )

                store.save(
                    WidgetSnapshot(
                        roomId = "room-widget-test",
                        roomTitle = "둘만의 그림방",
                        privacyMode = false,
                        latestNoteText = "오늘 보고 싶어",
                        dailySparkText = "오늘 서로에게 고마웠던 점 하나는?",
                        latestMemoryTitle = "첫 바다 산책",
                        latestMemoryId = "memory-widget-test",
                        latestMemoryImageUrl = "https://example.test/memory.jpg",
                        latestMemoryLocalPath = memoryPreviewPath,
                        latestDrawingUrl = "https://example.test/drawing.png",
                        latestDrawingLocalPath = previewPath,
                    ),
                )
                assertEquals("https://example.test/drawing.png", store.snapshot.first().latestDrawingUrl)
                assertEquals(previewPath, store.snapshot.first().latestDrawingLocalPath)
                assertEquals("https://example.test/memory.jpg", store.snapshot.first().latestMemoryImageUrl)
                assertEquals(memoryPreviewPath, store.snapshot.first().latestMemoryLocalPath)

                store.save(
                    WidgetSnapshot(
                        roomId = "room-widget-test",
                        roomTitle = "둘만의 그림방",
                        privacyMode = true,
                        nextDateText = "D-3 · 비밀 데이트",
                        latestNoteText = "민감한 노트",
                        dailySparkText = "민감한 질문",
                        latestMemoryTitle = "민감한 추억",
                        latestMemoryId = "memory-widget-test",
                        latestMemoryImageUrl = "https://example.test/memory.jpg",
                        latestDrawingUrl = null,
                    ),
                )
                val privateSnapshot = store.snapshot.first()

                assertEquals("새 노트가 있어요", privateSnapshot.latestNoteText)
                assertEquals("앱에서 다음 데이트를 확인해요", privateSnapshot.nextDateText)
                assertEquals("오늘의 질문이 도착했어요", privateSnapshot.dailySparkText)
                assertEquals("앱에서 추억을 확인해요", privateSnapshot.latestMemoryTitle)
                assertEquals("https://example.test/memory.jpg", privateSnapshot.latestMemoryImageUrl)
                assertEquals(memoryPreviewPath, privateSnapshot.latestMemoryLocalPath)
                assertEquals("https://example.test/drawing.png", privateSnapshot.latestDrawingUrl)
                assertEquals(previewPath, privateSnapshot.latestDrawingLocalPath)

                store.save(WidgetSnapshot(roomId = "other-room-widget-test"))
                val otherRoomSnapshot = store.snapshot.first()
                assertNull(otherRoomSnapshot.latestMemoryId)
                assertNull(otherRoomSnapshot.latestMemoryImageUrl)
                assertNull(otherRoomSnapshot.latestMemoryLocalPath)
                assertNull(otherRoomSnapshot.latestDrawingUrl)
                assertNull(otherRoomSnapshot.latestDrawingLocalPath)
            }
        }
    }

    @Test
    fun saveLatestDrawingPreviewStoresPrivatePngPath() {
        runBlocking {
            withTimeout(10_000) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val store = WidgetStateStore(context)
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

                val path = store.saveLatestDrawingPreview("room/with:unsafe", bitmap)
                val file = File(path)

                assertTrue(file.exists())
                assertTrue(file.name.startsWith("latest_drawing_room_with_unsafe"))
                assertTrue(file.readBytes().isNotEmpty())
            }
        }
    }

    @Test
    fun saveLatestMemoryPreviewStoresPrivateJpegPath() {
        runBlocking {
            withTimeout(10_000) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val store = WidgetStateStore(context)
                val bitmap = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)

                val path = store.saveLatestMemoryPreview("room/with:unsafe", "memory:unsafe", bitmap)
                val file = File(path)

                assertTrue(file.exists())
                assertTrue(file.name.startsWith("latest_memory_room_with_unsafe_memory_unsafe"))
                assertTrue(file.name.endsWith(".jpg"))
                assertTrue(file.readBytes().isNotEmpty())
            }
        }
    }

    @Test
    fun selectedWidgetRoomPreventsOtherRoomsFromOverwritingWidgetState() {
        runBlocking {
            withTimeout(10_000) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val store = WidgetStateStore(context)

                store.save(WidgetSnapshot(roomId = "widget-room-a", latestNoteText = "A의 노트"))

                assertEquals("widget-room-a", store.currentRoomId())
                assertTrue(store.shouldUpdateRoom("widget-room-a"))
                assertEquals(false, store.shouldUpdateRoom("widget-room-b"))

                store.save(WidgetSnapshot(roomId = "widget-room-b", latestNoteText = "B의 노트"))

                assertEquals("widget-room-b", store.currentRoomId())
                assertTrue(store.shouldUpdateRoom("widget-room-b"))
            }
        }
    }

    @Test
    fun remoteSyncedWidgetImagesAreCachedAsPrivatePreviewFiles() {
        runBlocking {
            withTimeout(10_000) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val store = WidgetStateStore(context)
                store.save(WidgetSnapshot(roomId = "cache-widget-room"))
                val source = File(context.cacheDir, "widget-cache-source.png")
                FileOutputStream(source).use { stream ->
                    Bitmap.createBitmap(10, 12, Bitmap.Config.ARGB_8888)
                        .compress(Bitmap.CompressFormat.PNG, 96, stream)
                }
                val sourceUrl = source.toURI().toString()

                val drawingPath = requireNotNull(store.cacheLatestDrawingPreviewFromUrl("cache-widget-room", sourceUrl))
                store.updateLatestDrawing("cache-widget-room", sourceUrl, drawingPath)
                val memoryPath = requireNotNull(store.cacheLatestMemoryPreviewFromUrl("cache-widget-room", "memory-cache", sourceUrl))
                store.updateLatestMemory("cache-widget-room", "원격 추억", "memory-cache", sourceUrl, memoryPath)
                val snapshot = store.snapshot.first()

                assertTrue(File(drawingPath).exists())
                assertTrue(File(memoryPath).exists())
                assertEquals(drawingPath, snapshot.latestDrawingLocalPath)
                assertEquals(memoryPath, snapshot.latestMemoryLocalPath)
            }
        }
    }
}
