package com.example.couplecanvas

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.CanvasEmoji
import com.example.couplecanvas.data.model.DrawingBackground
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.JoinRoomResult
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.data.repository.DrawingRepository
import com.example.couplecanvas.data.repository.RoomRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseRoomDrawingIntegrationTest {
    @Test
    fun createsRoomJoinsWithCodeAndSyncsStrokeThroughFirebase() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val drawingRepository = DrawingRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "통합 테스트 그림방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val mappedRoomId = firebase.root.child("roomCodes").child(roomCode).get().await().getValue(String::class.java)
                assertEquals(roomId, mappedRoomId)

                val joinResult = roomRepository.joinRoom(roomCode, guestUid)
                assertTrue("join result was $joinResult", joinResult is JoinRoomResult.Success)
                assertEquals(roomId, (joinResult as JoinRoomResult.Success).roomId)

                val stroke = Stroke(
                    strokeId = "stroke-${UUID.randomUUID()}",
                    ownerUid = guestUid,
                    color = "#FF7A9A",
                    width = 12f,
                    eraser = false,
                    createdAt = System.currentTimeMillis(),
                    points = mapOf(
                        "0000" to DrawingPoint(0.12f, 0.15f, 1L),
                        "0001" to DrawingPoint(0.42f, 0.45f, 2L),
                        "0002" to DrawingPoint(0.78f, 0.30f, 3L),
                    ),
                )

                drawingRepository.updateActiveStroke(roomId, guestUid, stroke)
                val activeStroke = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("activeStrokes")
                    .child(guestUid)
                    .get()
                    .await()
                    .getValue(Stroke::class.java)
                assertEquals(stroke.strokeId, activeStroke?.strokeId)

                drawingRepository.finishStroke(roomId, guestUid, stroke)
                val activeExists = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("activeStrokes")
                    .child(guestUid)
                    .get()
                    .await()
                    .exists()
                val finishedStroke = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("strokes")
                    .child(stroke.strokeId)
                    .get()
                    .await()
                    .getValue(Stroke::class.java)

                assertFalse(activeExists)
                assertEquals(stroke.strokeId, finishedStroke?.strokeId)
                assertEquals(3, finishedStroke?.points?.size)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
            }
        }
    }

    @Test
    fun syncsDrawingBackgroundAndEmojiObjectsThroughFirebase() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val authResult = firebase.auth.signInAnonymously().await()
                val hostUid = requireNotNull(authResult.user?.uid)
                val roomRepository = RoomRepository(firebase)
                val drawingRepository = DrawingRepository(firebase)
                val uploadFile = File.createTempFile("drawing-background-${UUID.randomUUID()}", ".jpg").apply {
                    writeBytes(byteArrayOf(1))
                }

                val roomId = roomRepository.createRoom(hostUid, title = "배경 이모지 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )

                val background = drawingRepository.setBackgroundImage(roomId, hostUid, Uri.fromFile(uploadFile))
                val emoji = drawingRepository.addEmoji(roomId, hostUid, "💗", x = 0.25f, y = 0.75f, size = 52f)

                val storedBackground = requireNotNull(
                    firebase.root
                        .child("rooms")
                        .child(roomId)
                        .child("drawingBackground")
                        .get()
                        .await()
                        .getValue(DrawingBackground::class.java),
                )
                val storedEmoji = requireNotNull(
                    firebase.root
                        .child("rooms")
                        .child(roomId)
                        .child("emojis")
                        .child(emoji.emojiId)
                        .get()
                        .await()
                        .getValue(CanvasEmoji::class.java),
                )

                assertTrue(background.storagePath.startsWith("rooms/$roomId/uploads/$hostUid/backgrounds/"))
                assertEquals(background.storagePath, storedBackground.storagePath)
                assertTrue(storedBackground.imageUrl.isNotBlank())
                assertEquals("💗", storedEmoji.emoji)
                assertEquals(0.25f, storedEmoji.x, 0.001f)
                assertEquals(0.75f, storedEmoji.y, 0.001f)

                drawingRepository.undoLastEmoji(roomId, hostUid)
                val emojiExistsAfterUndo = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("emojis")
                    .child(emoji.emojiId)
                    .get()
                    .await()
                    .exists()
                assertFalse(emojiExistsAfterUndo)

                uploadFile.delete()
                runCatching { firebase.storage.reference.child(background.storagePath).delete().await() }
                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.auth.signOut()
            }
        }
    }
}
