package com.example.couplecanvas

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DrawingPoint
import com.example.couplecanvas.data.model.JoinRoomResult
import com.example.couplecanvas.data.model.ModelFactory
import com.example.couplecanvas.data.model.Stroke
import com.example.couplecanvas.data.repository.DrawingRepository
import com.example.couplecanvas.data.repository.FeatureRepository
import com.example.couplecanvas.data.repository.RoomRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseProductionRulesSdkIntegrationTest {
    @Test
    fun authenticatedUsersCanCreateJoinAndSyncRoomDataUnderProductionRules() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(30_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val host = firebase.createTestAccount("host")
                val guest = firebase.createTestAccount("guest")
                val outsider = firebase.createTestAccount("outsider")
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)
                val drawingRepository = DrawingRepository(firebase)

                Log.i(TAG, "sign in host")
                firebase.signIn(host)
                Log.i(TAG, "create room")
                val roomId = roomRepository.createRoom(host.uid, title = "Production Rules SDK 방")
                Log.i(TAG, "read room code")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                Log.i(TAG, "write host note")
                featureRepository.sendNote(roomId, host.uid, "host note")
                val planId = "date-${UUID.randomUUID()}"
                Log.i(TAG, "write host date plan")
                featureRepository.saveDatePlan(
                    roomId,
                    DatePlan(
                        planId = planId,
                        title = "규칙 통과 데이트",
                        description = "실제 auth.uid로 저장되는 플랜",
                        createdAt = System.currentTimeMillis(),
                        createdByUid = host.uid,
                    ),
                )
                Log.i(TAG, "write host stroke")
                val stroke = Stroke(
                    strokeId = "stroke-${UUID.randomUUID()}",
                    ownerUid = host.uid,
                    color = "#FF7A9A",
                    width = 10f,
                    eraser = false,
                    createdAt = System.currentTimeMillis(),
                    points = mapOf(
                        "00000" to DrawingPoint(0.1f, 0.2f, 1L),
                        "00001" to DrawingPoint(0.8f, 0.7f, 2L),
                    ),
                )
                drawingRepository.updateActiveStroke(roomId, host.uid, stroke)
                drawingRepository.finishStroke(roomId, host.uid, stroke)

                Log.i(TAG, "sign in guest")
                firebase.signIn(guest)
                Log.i(TAG, "join as guest")
                val joinResult = roomRepository.joinRoom(roomCode, guest.uid)
                assertTrue("join result was $joinResult", joinResult is JoinRoomResult.Success)
                Log.i(TAG, "write guest note")
                featureRepository.sendNote(roomId, guest.uid, "guest note")
                val dateKey = ModelFactory.dateKey()
                Log.i(TAG, "ensure spark")
                featureRepository.ensureDailySpark(roomId, dateKey)
                Log.i(TAG, "answer spark")
                featureRepository.answerDailySpark(roomId, dateKey, guest.uid, "같이 테스트한 날")
                Log.i(TAG, "read guest room")
                val guestRoom = requireNotNull(firebase.root.child("rooms").child(roomId).get().await())
                assertEquals(2L, guestRoom.child("notes").childrenCount)

                Log.i(TAG, "sign in outsider")
                firebase.signIn(outsider)
                Log.i(TAG, "try outsider join")
                assertTrue(roomRepository.joinRoom(roomCode, outsider.uid) is JoinRoomResult.Full)
                Log.i(TAG, "try outsider read")
                val outsiderRead = runCatching { firebase.root.child("rooms").child(roomId).get().await() }
                if (InstrumentationRegistry.getArguments().getString("productionRules") == "true") {
                    assertTrue("Outsider should not read a room they did not join.", outsiderRead.isFailure)
                } else {
                    Log.i(TAG, "debug rules may allow outsider room reads; productionRules argument not set")
                }

                Log.i(TAG, "sign in host for final assertions")
                firebase.signIn(host)
                Log.i(TAG, "read final room")
                val roomSnapshot = firebase.root.child("rooms").child(roomId).get().await()
                assertEquals(2L, roomSnapshot.child("notes").childrenCount)
                assertTrue(roomSnapshot.child("datePlans").child(planId).exists())
                assertTrue(roomSnapshot.child("strokes").child(stroke.strokeId).exists())
                assertTrue(roomSnapshot.child("dailySparks").child(dateKey).child("answers").child(guest.uid).exists())

                Log.i(TAG, "close room")
                roomRepository.closeRoom(roomId, host.uid)
                firebase.auth.signOut()
            }
        }
    }

    private data class TestAccount(
        val uid: String,
        val email: String,
        val password: String,
    )

    private suspend fun FirebaseProvider.createTestAccount(label: String): TestAccount {
        val email = "$label-${UUID.randomUUID()}@couplecanvas.test"
        val password = "Passw0rd-${UUID.randomUUID()}!"
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = requireNotNull(result.user?.uid)
        auth.signOut()
        return TestAccount(uid = uid, email = email, password = password)
    }

    private suspend fun FirebaseProvider.signIn(account: TestAccount) {
        auth.signOut()
        auth.signInWithEmailAndPassword(account.email, account.password).await()
    }

    private companion object {
        const val TAG = "ProdRulesTest"
    }
}
