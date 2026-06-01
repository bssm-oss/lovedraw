package com.example.couplecanvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.couplecanvas.data.model.DateMode
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DateTone
import com.example.couplecanvas.data.model.MemoryItem
import com.example.couplecanvas.util.ShareCardGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ShareCardGeneratorIntegrationTest {
    @Test
    fun createsNineBySixteenDatePlanShareCardUri() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val plan = DatePlan(
            planId = "share-date-test",
            title = "비 오는 날 홈 시네마",
            description = "외부 장소 검색 없이 집에서 안전하게 즐기는 데이트 플랜",
            vibe = "Movie Night",
            mode = DateMode.Home.value,
            tone = DateTone.Playful.value,
            estimatedTime = "1시간",
            estimatedBudget = "낮음",
            steps = listOf("서로 보고 싶은 영화를 하나씩 고르기", "간단한 간식을 같이 준비하기", "마지막에 한 줄 감상 남기기"),
        )

        val uri = ShareCardGenerator.createDatePlanCardUri(context, "둘만의 그림방", plan)
        val bitmap = context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input) }

        assertNotNull(bitmap)
        assertEquals(ShareCardGenerator.CARD_WIDTH, bitmap.width)
        assertEquals(ShareCardGenerator.CARD_HEIGHT, bitmap.height)
    }

    @Test
    fun createsNineBySixteenMemoryShareCardUriWithoutPhotos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val memory = MemoryItem(
            memoryId = "share-memory-test",
            title = "첫 바다 산책",
            note = "바람이 좋아서 오래 기억하고 싶은 날",
            date = 1_800_000_000_000L,
        )

        val uri = ShareCardGenerator.createMemoryCardUri(context, "둘만의 그림방", memory)
        val bitmap = context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input) }

        assertNotNull(bitmap)
        assertEquals(ShareCardGenerator.CARD_WIDTH, bitmap.width)
        assertEquals(ShareCardGenerator.CARD_HEIGHT, bitmap.height)
    }

    @Test
    fun createsNineBySixteenMemoryShareCardUriWithLocalPhotos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val memory = MemoryItem(
            memoryId = "share-memory-local-photo-test",
            title = "컬러 추억",
            note = "로컬 파일 사진으로도 카드가 만들어져야 해요",
            date = 1_800_000_000_000L,
            imageUrls = listOf(
                createSolidPhotoUri("share-memory-red", Color.RED),
                createSolidPhotoUri("share-memory-blue", Color.BLUE),
                createSolidPhotoUri("share-memory-green", Color.GREEN),
            ),
        )

        val uri = ShareCardGenerator.createMemoryCardUri(context, "둘만의 그림방", memory)
        val bitmap = context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input) }

        assertNotNull(bitmap)
        assertEquals(ShareCardGenerator.CARD_WIDTH, bitmap.width)
        assertEquals(ShareCardGenerator.CARD_HEIGHT, bitmap.height)
        assertEquals(Color.RED, bitmap.getPixel(260, 680))
    }

    @Test
    fun createsSquareMemoryCollageUriWithLocalPhotos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val memory = MemoryItem(
            memoryId = "memory-collage-local-photo-test",
            title = "콜라주 추억",
            date = 1_800_000_000_000L,
            imageUrls = listOf(
                createSolidPhotoUri("collage-red", Color.RED),
                createSolidPhotoUri("collage-blue", Color.BLUE),
                createSolidPhotoUri("collage-green", Color.GREEN),
                createSolidPhotoUri("collage-yellow", Color.YELLOW),
            ),
        )

        val uri = ShareCardGenerator.createMemoryCollageUri(context, memory)
        val bitmap = context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input) }

        assertNotNull(bitmap)
        assertEquals(ShareCardGenerator.COLLAGE_SIZE, bitmap.width)
        assertEquals(ShareCardGenerator.COLLAGE_SIZE, bitmap.height)
        assertEquals(Color.RED, bitmap.getPixel(280, 280))
        assertEquals(Color.BLUE, bitmap.getPixel(800, 280))
        assertEquals(Color.GREEN, bitmap.getPixel(280, 800))
        assertEquals(Color.YELLOW, bitmap.getPixel(800, 800))
    }

    private fun createSolidPhotoUri(name: String, color: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "$name.png")
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        FileOutputStream(file).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()
        return Uri.fromFile(file).toString()
    }
}
