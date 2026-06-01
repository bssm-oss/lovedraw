package com.example.couplecanvas

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.couplecanvas.data.firebase.FirebaseProvider
import com.example.couplecanvas.data.model.BucketItem
import com.example.couplecanvas.data.model.BucketStatus
import com.example.couplecanvas.data.model.DailySpark
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DateVote
import com.example.couplecanvas.data.model.DrawingSnapshot
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.data.model.LocationShareState
import com.example.couplecanvas.data.model.MemoryItem
import com.example.couplecanvas.data.model.ModelFactory
import com.example.couplecanvas.data.model.QuizAnswer
import com.example.couplecanvas.data.model.QuizDiscussion
import com.example.couplecanvas.data.model.JoinRoomResult
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.data.repository.FeatureRepository
import com.example.couplecanvas.data.repository.RoomRepository
import com.example.couplecanvas.util.DatePlanMatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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
class FirebaseFeatureRepositoryIntegrationTest {
    @Test
    fun managesMultipleRoomsPerUserWithIsolatedRecordsArchiveAndReopen() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val outsiderUid = "outsider-${UUID.randomUUID()}"
                val firstRoomId = roomRepository.createRoom(hostUid, title = "첫 번째 기록방")
                val secondRoomId = roomRepository.createRoom(hostUid, title = "두 번째 기록방")
                val firstRoomCode = requireNotNull(
                    firebase.root.child("rooms").child(firstRoomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val secondRoomCode = requireNotNull(
                    firebase.root.child("rooms").child(secondRoomId).child("roomCode").get().await().getValue(String::class.java),
                )

                assertTrue(roomRepository.joinRoom(firstRoomCode, guestUid) is JoinRoomResult.Success)
                featureRepository.sendNote(firstRoomId, guestUid, "첫 번째 방에만 남긴 노트")
                featureRepository.saveDatePlan(
                    secondRoomId,
                    DatePlan(
                        planId = "date-${UUID.randomUUID()}",
                        title = "두 번째 방 데이트",
                        description = "방별로 분리된 데이트 플랜",
                        createdAt = System.currentTimeMillis(),
                        createdByUid = hostUid,
                    ),
                )

                val activeSummaries = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any { it.room.roomId == firstRoomId && it.noteCount == 1 && it.datePlanCount == 0 } &&
                        list.any { it.room.roomId == secondRoomId && it.noteCount == 0 && it.datePlanCount == 1 }
                }
                val firstSummary = activeSummaries.single { it.room.roomId == firstRoomId }
                val secondSummary = activeSummaries.single { it.room.roomId == secondRoomId }
                assertEquals("첫 번째 기록방", firstSummary.room.title)
                assertEquals("두 번째 기록방", secondSummary.room.title)
                assertEquals(1, firstSummary.noteCount)
                assertEquals(0, firstSummary.datePlanCount)
                assertEquals(0, secondSummary.noteCount)
                assertEquals(1, secondSummary.datePlanCount)

                roomRepository.closeRoom(firstRoomId, hostUid)
                val closedSummaries = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any { it.room.roomId == firstRoomId && it.room.status == RoomStatus.Closed.value } &&
                        list.any { it.room.roomId == secondRoomId && it.room.status == RoomStatus.Waiting.value }
                }
                assertEquals(RoomStatus.Closed.value, closedSummaries.single { it.room.roomId == firstRoomId }.room.status)
                assertEquals(null, firebase.root.child("roomCodes").child(firstRoomCode).get().await().getValue(String::class.java))
                assertTrue(roomRepository.joinRoom(firstRoomCode, outsiderUid) is JoinRoomResult.NotFound)

                val reopenedCode = roomRepository.reopenRoom(firstRoomId, hostUid)
                val reopenedRoom = requireNotNull(firebase.root.child("rooms").child(firstRoomId).get().await().getValue(Room::class.java))
                assertEquals(RoomStatus.Active.value, reopenedRoom.status)
                assertEquals(firstRoomId, firebase.root.child("roomCodes").child(reopenedCode).get().await().getValue(String::class.java))
                assertTrue(roomRepository.joinRoom(reopenedCode, outsiderUid) is JoinRoomResult.Full)

                firebase.root.child("rooms").child(firstRoomId).removeValue().await()
                firebase.root.child("rooms").child(secondRoomId).removeValue().await()
                firebase.root.child("roomCodes").child(firstRoomCode).removeValue().await()
                firebase.root.child("roomCodes").child(secondRoomCode).removeValue().await()
                firebase.root.child("roomCodes").child(reopenedCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
                firebase.root.child("userRooms").child(outsiderUid).removeValue().await()
            }
        }
    }

    @Test
    fun observesRoomHomeSummariesWithRoomScopedRecordCounts() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "기록 보관함 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)
                val updatedSummaries = async {
                    roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                        list.any {
                            it.room.roomId == roomId &&
                                it.noteCount == 2 &&
                                it.unreadNoteCount == 1 &&
                                it.datePlanCount == 1 &&
                                it.memoryCount == 1 &&
                                it.drawingSnapshotCount == 1
                        }
                    }
                }
                delay(250)
                featureRepository.sendNote(roomId, hostUid, "홈에 보이는 노트")
                featureRepository.sendNote(roomId, guestUid, "상대방이 보낸 새 노트")
                featureRepository.saveDatePlan(
                    roomId,
                    DatePlan(
                        planId = "date-${UUID.randomUUID()}",
                        title = "주말 산책",
                        description = "가볍게 산책하고 기록 남기기",
                        createdAt = System.currentTimeMillis(),
                        createdByUid = hostUid,
                    ),
                )
                featureRepository.saveMemory(roomId, hostUid, "첫 기록", "홈 요약에 보이는 추억", System.currentTimeMillis(), emptyList())
                featureRepository.ensureDailySpark(roomId, "2026-05-28")
                firebase.root.child("rooms").child(roomId).child("drawingSnapshots").child("drawing-1").setValue(
                    DrawingSnapshot(
                        drawingId = "drawing-1",
                        authorUid = hostUid,
                        imageUrl = "https://example.invalid/drawing.png",
                        storagePath = "rooms/$roomId/uploads/$hostUid/drawings/drawing-1.png",
                        createdAt = System.currentTimeMillis(),
                    ),
                ).await()

                val summaries = updatedSummaries.await()
                val summary = summaries.single { it.room.roomId == roomId }

                assertEquals("기록 보관함 테스트 방", summary.room.title)
                assertEquals(2, summary.noteCount)
                assertEquals(1, summary.unreadNoteCount)
                assertEquals(1, summary.datePlanCount)
                assertEquals(1, summary.memoryCount)
                assertEquals(1, summary.drawingSnapshotCount)
                assertEquals(1, summary.dailySparkCount)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun marksOnlyReceivedLoveNotesAsReadAndRefreshesUnreadSummary() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "읽음 처리 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)

                featureRepository.sendNote(roomId, hostUid, "내가 쓴 노트")
                featureRepository.sendNote(roomId, guestUid, "상대가 보낸 노트")

                val beforeSummary = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any { it.room.roomId == roomId && it.noteCount == 2 && it.unreadNoteCount == 1 }
                }.single { it.room.roomId == roomId }
                assertEquals(1, beforeSummary.unreadNoteCount)

                featureRepository.markReceivedNotesRead(roomId, hostUid)

                val notes = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("notes")
                    .get()
                    .await()
                    .children
                    .mapNotNull { it.getValue(com.example.couplecanvas.data.model.LoveNote::class.java) }
                val hostNote = notes.single { it.authorUid == hostUid }
                val guestNote = notes.single { it.authorUid == guestUid }
                assertEquals(false, hostNote.isRead)
                assertEquals(true, guestNote.isRead)

                val afterSummary = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any { it.room.roomId == roomId && it.noteCount == 2 && it.unreadNoteCount == 0 }
                }.single { it.room.roomId == roomId }
                assertEquals(0, afterSummary.unreadNoteCount)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun rejoiningExistingRoomPreservesOriginalMemberRoles() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "역할 보존 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )

                assertTrue(roomRepository.joinRoom(roomCode, hostUid) is JoinRoomResult.Success)
                val hostOnlyRoom = requireNotNull(firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java))
                val hostUserAfterRejoin = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("users").child(hostUid).get().await().getValue(com.example.couplecanvas.data.model.RoomUser::class.java),
                )
                assertEquals(hostUid, hostOnlyRoom.hostUid)
                assertEquals(null, hostOnlyRoom.guestUid)
                assertEquals(RoomStatus.Waiting.value, hostOnlyRoom.status)
                assertEquals("host", hostUserAfterRejoin.role)

                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)
                assertTrue(roomRepository.joinRoom(roomCode, hostUid) is JoinRoomResult.Success)

                val activeRoom = requireNotNull(firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java))
                val hostUser = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("users").child(hostUid).get().await().getValue(com.example.couplecanvas.data.model.RoomUser::class.java),
                )
                val guestUser = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("users").child(guestUid).get().await().getValue(com.example.couplecanvas.data.model.RoomUser::class.java),
                )

                assertEquals(RoomStatus.Active.value, activeRoom.status)
                assertEquals(2, activeRoom.activeUserCount)
                assertEquals(hostUid, activeRoom.hostUid)
                assertEquals(guestUid, activeRoom.guestUid)
                assertEquals("host", hostUser.role)
                assertEquals("guest", guestUser.role)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun concurrentJoinRoomKeepsRoomAtTwoMembers() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val firstGuestUid = "guest-${UUID.randomUUID()}"
                val secondGuestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "동시 입장 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )

                val firstJoin = async { roomRepository.joinRoom(roomCode, firstGuestUid) }
                val secondJoin = async { roomRepository.joinRoom(roomCode, secondGuestUid) }
                val results = listOf(firstJoin.await(), secondJoin.await())
                val success = results.single { it is JoinRoomResult.Success } as JoinRoomResult.Success
                assertEquals(roomId, success.roomId)
                assertEquals(1, results.count { it is JoinRoomResult.Full })

                val room = requireNotNull(firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java))
                val activeMembers = room.members.filterValues { it }.keys
                assertEquals(2, activeMembers.size)
                assertEquals(2, room.activeUserCount)
                assertTrue(room.guestUid in setOf(firstGuestUid, secondGuestUid))
                assertTrue(activeMembers.contains(hostUid))
                assertTrue(activeMembers.contains(room.guestUid))
                val rejectedGuestUid = setOf(firstGuestUid, secondGuestUid).single { it != room.guestUid }
                assertFalse(firebase.root.child("userRooms").child(rejectedGuestUid).child(roomId).get().await().exists())

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(firstGuestUid).removeValue().await()
                firebase.root.child("userRooms").child(secondGuestUid).removeValue().await()
            }
        }
    }

    @Test
    fun closesAndReopensRoomWhileKeepingRoomScopedHistory() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "보관 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                featureRepository.sendNote(roomId, hostUid, "보관해도 남는 노트")

                roomRepository.closeRoom(roomId, hostUid)
                val closedRoom = firebase.root.child("rooms").child(roomId).get().await()
                assertEquals(RoomStatus.Closed.value, closedRoom.child("status").getValue(String::class.java))
                assertEquals(false, firebase.root.child("roomCodes").child(roomCode).get().await().exists())
                assertEquals(JoinRoomResult.NotFound, roomRepository.joinRoom(roomCode, guestUid))

                val archivedSummary = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any {
                        it.room.roomId == roomId &&
                            it.room.status == RoomStatus.Closed.value &&
                            it.noteCount == 1
                    }
                }.single { it.room.roomId == roomId }
                assertEquals("보관 테스트 방", archivedSummary.room.title)
                assertEquals(1, archivedSummary.noteCount)

                val reopenedCode = roomRepository.reopenRoom(roomId, hostUid)
                val reopenedRoom = firebase.root.child("rooms").child(roomId).get().await()
                assertEquals(RoomStatus.Waiting.value, reopenedRoom.child("status").getValue(String::class.java))
                assertEquals(roomId, firebase.root.child("roomCodes").child(reopenedCode).get().await().getValue(String::class.java))
                assertTrue(roomRepository.joinRoom(reopenedCode, guestUid) is JoinRoomResult.Success)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(reopenedCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun leavingRoomRemovesOnlyCurrentUsersHomeMembershipAndKeepsRemainingRoomJoinable() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "나가기 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)

                roomRepository.leaveRoom(roomId, guestUid)

                val roomSnapshot = firebase.root.child("rooms").child(roomId).get().await()
                val room = requireNotNull(roomSnapshot.getValue(Room::class.java))
                assertEquals(RoomStatus.Waiting.value, room.status)
                assertEquals(1, room.activeUserCount)
                assertEquals(mapOf(hostUid to true), room.members)
                assertEquals(hostUid, room.hostUid)
                assertEquals(null, room.guestUid)
                assertEquals(roomId, firebase.root.child("roomCodes").child(roomCode).get().await().getValue(String::class.java))
                assertFalse(firebase.root.child("userRooms").child(guestUid).child(roomId).get().await().exists())

                val hostSummary = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any { it.room.roomId == roomId && it.room.status == RoomStatus.Waiting.value }
                }.single { it.room.roomId == roomId }
                assertEquals("나가기 테스트 방", hostSummary.room.title)
                val guestSummaries = roomRepository.observeRoomSummariesForUser(guestUid).first()
                assertTrue(guestSummaries.none { it.room.roomId == roomId })

                val newGuestUid = "guest-${UUID.randomUUID()}"
                assertTrue(roomRepository.joinRoom(roomCode, newGuestUid) is JoinRoomResult.Success)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
                firebase.root.child("userRooms").child(newGuestUid).removeValue().await()
            }
        }
    }

    @Test
    fun leavingAsLastMemberClosesRoomAndRemovesInviteCodeFromHome() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "마지막 나가기 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )

                roomRepository.leaveRoom(roomId, hostUid)

                val room = requireNotNull(firebase.root.child("rooms").child(roomId).get().await().getValue(Room::class.java))
                assertEquals(RoomStatus.Closed.value, room.status)
                assertEquals(0, room.activeUserCount)
                assertEquals(emptyMap<String, Boolean>(), room.members)
                assertFalse(firebase.root.child("roomCodes").child(roomCode).get().await().exists())
                assertFalse(firebase.root.child("userRooms").child(hostUid).child(roomId).get().await().exists())
                val summaries = roomRepository.observeRoomSummariesForUser(hostUid).first()
                assertTrue(summaries.none { it.room.roomId == roomId })

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
            }
        }
    }

    @Test
    fun deletesRoomScopedRecordsAndUpdatesHomeSummaryCounts() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "삭제 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                featureRepository.sendNote(roomId, hostUid, "삭제할 노트")
                val noteId = firebase.root.child("rooms").child(roomId).child("notes").get().await().children.single().key.orEmpty()
                val planId = "date-${UUID.randomUUID()}"
                featureRepository.saveDatePlan(
                    roomId,
                    DatePlan(
                        planId = planId,
                        title = "삭제할 플랜",
                        description = "삭제 후 홈 요약에서 빠져야 하는 플랜",
                        createdAt = System.currentTimeMillis(),
                        createdByUid = hostUid,
                    ),
                )
                val memory = featureRepository.saveMemory(roomId, hostUid, "삭제할 추억", null, System.currentTimeMillis(), emptyList())
                val bucketId = "bucket-${UUID.randomUUID()}"
                featureRepository.saveBucketItem(
                    roomId,
                    com.example.couplecanvas.data.model.BucketItem(
                        itemId = bucketId,
                        title = "삭제할 버킷",
                        createdByUid = hostUid,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )

                featureRepository.deleteNote(roomId, noteId)
                featureRepository.deleteDatePlan(roomId, planId)
                featureRepository.deleteMemory(roomId, memory)
                featureRepository.deleteBucketItem(roomId, bucketId)

                val roomSnapshot = firebase.root.child("rooms").child(roomId).get().await()
                assertEquals(false, roomSnapshot.child("notes").child(noteId).exists())
                assertEquals(false, roomSnapshot.child("datePlans").child(planId).exists())
                assertEquals(false, roomSnapshot.child("memories").child(memory.memoryId).exists())
                assertEquals(false, roomSnapshot.child("bucketList").child(bucketId).exists())

                val summaries = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any {
                        it.room.roomId == roomId &&
                            it.noteCount == 0 &&
                            it.datePlanCount == 0 &&
                            it.memoryCount == 0 &&
                            it.bucketItemCount == 0
                    }
                }
                val summary = summaries.single { it.room.roomId == roomId }
                assertEquals(0, summary.noteCount)
                assertEquals(0, summary.datePlanCount)
                assertEquals(0, summary.memoryCount)
                assertEquals(0, summary.bucketItemCount)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
            }
        }
    }

    @Test
    fun convertsBucketItemIntoDatePlanAndKeepsCrossReference() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "버킷 플랜 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val bucket = BucketItem(
                    itemId = "bucket-${UUID.randomUUID()}",
                    title = "새 산책길 걷기",
                    description = "가깝고 안전한 루트로 같이 걷기",
                    vibe = "Walk & Talk",
                    createdByUid = hostUid,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                val plan = DatePlan(
                    planId = "date-${UUID.randomUUID()}",
                    title = bucket.title,
                    description = bucket.description.orEmpty(),
                    vibe = bucket.vibe.orEmpty(),
                    estimatedBudget = "보통",
                    estimatedTime = "1시간",
                    steps = listOf("날짜 정하기", "같이 걷기", "추억 남기기"),
                    status = DatePlanStatus.Saved.value,
                    createdByUid = hostUid,
                    createdAt = System.currentTimeMillis(),
                )

                featureRepository.saveBucketItem(roomId, bucket)
                featureRepository.planBucketItem(roomId, bucket, plan)

                val roomSnapshot = firebase.root.child("rooms").child(roomId).get().await()
                val storedBucket = requireNotNull(roomSnapshot.child("bucketList").child(bucket.itemId).getValue(BucketItem::class.java))
                val storedPlan = requireNotNull(roomSnapshot.child("datePlans").child(plan.planId).getValue(DatePlan::class.java))

                assertEquals(BucketStatus.Planned.value, storedBucket.status)
                assertEquals(plan.planId, storedBucket.plannedDatePlanId)
                assertEquals(bucket.title, storedPlan.title)
                assertEquals("Walk & Talk", storedPlan.vibe)
                assertEquals(3, storedPlan.steps.size)

                val summary = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any {
                        it.room.roomId == roomId &&
                            it.bucketItemCount == 1 &&
                            it.datePlanCount == 1
                    }
                }.single { it.room.roomId == roomId }
                assertEquals(1, summary.bucketItemCount)
                assertEquals(1, summary.datePlanCount)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
            }
        }
    }

    @Test
    fun voteDatePlanMarksMatchedOnlyWhenBothRoomMembersLikeIt() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "투표 매칭 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)
                val planId = "date-${UUID.randomUUID()}"
                featureRepository.saveDatePlan(
                    roomId,
                    DatePlan(
                        planId = planId,
                        title = "같이 고르는 산책",
                        description = "둘 다 좋아요를 눌러야 매칭되는 플랜",
                        vibe = "Walk & Talk",
                        estimatedBudget = "낮음",
                        estimatedTime = "1시간",
                        steps = listOf("후보 보기", "투표하기", "예정으로 옮기기"),
                        createdAt = System.currentTimeMillis(),
                        createdByUid = hostUid,
                    ),
                )

                featureRepository.voteDatePlan(roomId, planId, hostUid, DateVote.Like.value)
                val hostOnlyPlan = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("datePlans").child(planId).get().await().getValue(DatePlan::class.java),
                )
                assertEquals(mapOf(hostUid to DateVote.Like.value), hostOnlyPlan.votes)
                assertEquals(null, hostOnlyPlan.matchedAt)
                assertEquals(emptyMap<String, Boolean>(), hostOnlyPlan.matchedBy)

                featureRepository.voteDatePlan(roomId, planId, guestUid, DateVote.Later.value)
                val laterPlan = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("datePlans").child(planId).get().await().getValue(DatePlan::class.java),
                )
                assertEquals(DateVote.Later.value, laterPlan.votes[guestUid])
                assertEquals(null, laterPlan.matchedAt)

                featureRepository.voteDatePlan(roomId, planId, guestUid, DateVote.Like.value)
                val matchedSnapshot = firebase.root.child("rooms").child(roomId).get().await()
                val matchedPlan = requireNotNull(matchedSnapshot.child("datePlans").child(planId).getValue(DatePlan::class.java))
                val room = requireNotNull(matchedSnapshot.getValue(Room::class.java))

                assertEquals(DateVote.Like.value, matchedPlan.votes[hostUid])
                assertEquals(DateVote.Like.value, matchedPlan.votes[guestUid])
                assertTrue(requireNotNull(matchedPlan.matchedAt) > 0L)
                assertEquals(mapOf(hostUid to true, guestUid to true), matchedPlan.matchedBy)
                assertTrue(DatePlanMatcher.isMatched(matchedPlan, room))

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun editsRoomScopedRecordsWithoutDroppingMetadata() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "편집 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)

                featureRepository.sendNote(roomId, hostUid, "처음 노트")
                val noteId = firebase.root.child("rooms").child(roomId).child("notes").get().await().children.single().key.orEmpty()
                featureRepository.togglePin(
                    roomId,
                    requireNotNull(
                        firebase.root.child("rooms").child(roomId).child("notes").child(noteId).get().await().getValue(com.example.couplecanvas.data.model.LoveNote::class.java),
                    ),
                )

                val planId = "date-${UUID.randomUUID()}"
                featureRepository.saveDatePlan(
                    roomId,
                    DatePlan(
                        planId = planId,
                        title = "처음 플랜",
                        description = "처음 설명",
                        vibe = "Cozy",
                        estimatedBudget = "낮음",
                        estimatedTime = "30분",
                        steps = listOf("준비", "기록"),
                        createdAt = System.currentTimeMillis(),
                        createdByUid = hostUid,
                    ),
                )
                featureRepository.voteDatePlan(roomId, planId, hostUid, "like")
                val scheduledAt = 1_802_000_000_000L
                featureRepository.updateDateStatus(roomId, planId, com.example.couplecanvas.data.model.DatePlanStatus.Upcoming, scheduledAt)

                val memory = featureRepository.saveMemory(roomId, hostUid, "처음 추억", "처음 메모", 1_800_000_000_000L, emptyList())
                val bucketId = "bucket-${UUID.randomUUID()}"
                featureRepository.saveBucketItem(
                    roomId,
                    com.example.couplecanvas.data.model.BucketItem(
                        itemId = bucketId,
                        title = "처음 버킷",
                        description = "처음 설명",
                        vibe = "Cozy",
                        createdByUid = hostUid,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                featureRepository.updateBucketStatus(roomId, bucketId, com.example.couplecanvas.data.model.BucketStatus.Planned.value)

                featureRepository.updateNote(roomId, noteId, "수정된 노트")
                featureRepository.updateDatePlan(
                    roomId,
                    DatePlan(
                        planId = planId,
                        title = "수정된 플랜",
                        description = "수정된 설명",
                        vibe = "Creative",
                        estimatedBudget = "보통",
                        estimatedTime = "1시간",
                        steps = listOf("새 준비", "서로 기록"),
                        scheduledAt = scheduledAt,
                    ),
                )
                featureRepository.updateMemory(roomId, memory.memoryId, "수정된 추억", "수정된 메모", 1_801_000_000_000L)
                featureRepository.updateBucketItem(roomId, bucketId, "수정된 버킷", "수정된 설명", "Adventure")

                val roomSnapshot = firebase.root.child("rooms").child(roomId).get().await()
                val editedNote = requireNotNull(roomSnapshot.child("notes").child(noteId).getValue(com.example.couplecanvas.data.model.LoveNote::class.java))
                val editedPlan = requireNotNull(roomSnapshot.child("datePlans").child(planId).getValue(DatePlan::class.java))
                val editedMemory = requireNotNull(roomSnapshot.child("memories").child(memory.memoryId).getValue(MemoryItem::class.java))
                val editedBucket = requireNotNull(roomSnapshot.child("bucketList").child(bucketId).getValue(com.example.couplecanvas.data.model.BucketItem::class.java))

                assertEquals("수정된 노트", editedNote.message)
                assertEquals(true, editedNote.isPinned)
                assertTrue(editedNote.updatedAt >= editedNote.createdAt)

                assertEquals("수정된 플랜", editedPlan.title)
                assertEquals("수정된 설명", editedPlan.description)
                assertEquals("Creative", editedPlan.vibe)
                assertEquals("보통", editedPlan.estimatedBudget)
                assertEquals("1시간", editedPlan.estimatedTime)
                assertEquals(listOf("새 준비", "서로 기록"), editedPlan.steps)
                assertEquals(com.example.couplecanvas.data.model.DatePlanStatus.Upcoming.value, editedPlan.status)
                assertEquals(scheduledAt, requireNotNull(editedPlan.scheduledAt))
                assertEquals(mapOf(hostUid to "like"), editedPlan.votes)

                assertEquals("수정된 추억", editedMemory.title)
                assertEquals("수정된 메모", editedMemory.note)
                assertEquals(1_801_000_000_000L, editedMemory.date)
                assertEquals(memory.storagePaths, editedMemory.storagePaths)

                assertEquals("수정된 버킷", editedBucket.title)
                assertEquals("수정된 설명", editedBucket.description)
                assertEquals("Adventure", editedBucket.vibe)
                assertEquals(com.example.couplecanvas.data.model.BucketStatus.Planned.value, editedBucket.status)

                val summaries = roomRepository.observeRoomSummariesForUser(hostUid).first { list ->
                    list.any {
                        it.room.roomId == roomId &&
                            it.noteCount == 1 &&
                            it.datePlanCount == 1 &&
                            it.memoryCount == 1 &&
                            it.bucketItemCount == 1
                    }
                }
                val summary = summaries.single { it.room.roomId == roomId }
                assertEquals(1, summary.noteCount)
                assertEquals(1, summary.datePlanCount)
                assertEquals(1, summary.memoryCount)
                assertEquals(1, summary.bucketItemCount)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun savesMemoryAndTouchesRoomWithoutSelectedImages() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "추억 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val beforeUpdatedAt = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("updatedAt").get().await().getValue(Long::class.java),
                )

                val saved = featureRepository.saveMemory(
                    roomId = roomId,
                    uid = hostUid,
                    title = "  첫 바다 산책  ",
                    note = "  바람이 좋았던 날  ",
                    date = 1_800_000_000_000L,
                    imageUris = emptyList(),
                )
                val stored = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("memories")
                    .child(saved.memoryId)
                    .get()
                    .await()
                    .getValue(MemoryItem::class.java)
                val afterUpdatedAt = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("updatedAt").get().await().getValue(Long::class.java),
                )

                assertEquals(saved.memoryId, stored?.memoryId)
                assertEquals("첫 바다 산책", stored?.title)
                assertEquals("바람이 좋았던 날", stored?.note)
                assertTrue(stored?.imageUrls?.isEmpty() == true)
                assertTrue(afterUpdatedAt >= beforeUpdatedAt)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
            }
        }
    }

    @Test
    fun addsAndRemovesSingleMemoryImageFromStorageAndDatabase() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(30_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val authResult = firebase.auth.signInAnonymously().await()
                val hostUid = requireNotNull(authResult.user?.uid)
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)
                val uploadFile = createTempUploadFile("memory-${UUID.randomUUID()}.jpg")

                val roomId = roomRepository.createRoom(hostUid, title = "사진 관리 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val memory = featureRepository.saveMemory(
                    roomId = roomId,
                    uid = hostUid,
                    title = "사진을 붙일 추억",
                    note = "개별 사진 삭제까지 검증",
                    date = 1_800_000_000_000L,
                    imageUris = emptyList(),
                )

                val added = featureRepository.addMemoryImages(roomId, hostUid, memory, listOf(Uri.fromFile(uploadFile)))
                val uploadedPath = added.storagePaths.single()
                val uploadedBytes = firebase.storage.reference.child(uploadedPath).getBytes(1024).await()
                val storedAfterAdd = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("memories").child(memory.memoryId).get().await().getValue(MemoryItem::class.java),
                )

                assertEquals(1, uploadedBytes.size)
                assertEquals(1, storedAfterAdd.imageUrls.size)
                assertEquals(1, storedAfterAdd.storagePaths.size)
                assertTrue(uploadedPath.startsWith("rooms/$roomId/uploads/$hostUid/memories/${memory.memoryId}/"))

                val removed = featureRepository.removeMemoryImage(roomId, storedAfterAdd, 0)
                val storedAfterRemove = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("memories").child(memory.memoryId).get().await().getValue(MemoryItem::class.java),
                )

                assertTrue(removed.imageUrls.isEmpty())
                assertTrue(removed.storagePaths.isEmpty())
                assertTrue(storedAfterRemove.imageUrls.isEmpty())
                assertTrue(storedAfterRemove.storagePaths.isEmpty())
                assertTrue(runCatching { firebase.storage.reference.child(uploadedPath).metadata.await() }.isFailure)

                uploadFile.delete()
                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.auth.signOut()
            }
        }
    }

    @Test
    fun concurrentMemoryImageAddsNeverStoreMoreThanFourImages() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(40_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val authResult = firebase.auth.signInAnonymously().await()
                val hostUid = requireNotNull(authResult.user?.uid)
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)
                val uploadFiles = (0 until 8).map { index ->
                    createTempUploadFile("memory-concurrent-$index-${UUID.randomUUID()}.jpg")
                }

                val roomId = roomRepository.createRoom(hostUid, title = "동시 사진 추가 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val memory = featureRepository.saveMemory(
                    roomId = roomId,
                    uid = hostUid,
                    title = "사진 최대 개수 테스트",
                    note = null,
                    date = 1_800_000_000_000L,
                    imageUris = emptyList(),
                )

                val firstBatch = uploadFiles.take(4).map { Uri.fromFile(it) }
                val secondBatch = uploadFiles.drop(4).map { Uri.fromFile(it) }
                val firstAdd = async { runCatching { featureRepository.addMemoryImages(roomId, hostUid, memory, firstBatch) } }
                val secondAdd = async { runCatching { featureRepository.addMemoryImages(roomId, hostUid, memory, secondBatch) } }
                val outcomes = listOf(firstAdd.await(), secondAdd.await())
                val successfulMemories = outcomes.mapNotNull { it.getOrNull() }
                assertTrue(outcomes.any { it.isSuccess })
                val stored = requireNotNull(successfulMemories.maxByOrNull { it.imageUrls.size })

                successfulMemories.forEach { updatedMemory ->
                    assertTrue(updatedMemory.imageUrls.size <= 4)
                    assertEquals(updatedMemory.imageUrls.size, updatedMemory.storagePaths.size)
                }
                assertTrue(stored.imageUrls.size <= 4)
                assertEquals(stored.imageUrls.size, stored.storagePaths.size)
                stored.storagePaths.forEach { path ->
                    assertEquals(1, firebase.storage.reference.child(path).getBytes(1024).await().size)
                }

                stored.storagePaths.forEach { path -> runCatching { firebase.storage.reference.child(path).delete().await() } }
                uploadFiles.forEach { it.delete() }
                runCatching { firebase.root.child("rooms").child(roomId).removeValue().await() }
                runCatching { firebase.root.child("roomCodes").child(roomCode).removeValue().await() }
                runCatching { firebase.root.child("userRooms").child(hostUid).removeValue().await() }
                firebase.auth.signOut()
            }
        }
    }

    @Test
    fun updatesLocationShareOnlyAfterExplicitOptInAndOneShotShare() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val roomId = roomRepository.createRoom(hostUid, title = "거리 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)

                featureRepository.setLocationSharingEnabled(roomId, hostUid, true)
                var state = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("locationShares")
                    .child(hostUid)
                    .get()
                    .await()
                    .getValue(LocationShareState::class.java)
                assertEquals(hostUid, state?.uid)
                assertEquals(true, state?.enabled)
                assertEquals(null, state?.latitude)

                val singleConsentShare = runCatching {
                    featureRepository.shareCurrentLocation(
                        roomId = roomId,
                        uid = hostUid,
                        latitude = 37.5665,
                        longitude = 126.9780,
                        accuracyMeters = 24f,
                        approximateOnly = false,
                    )
                }
                assertTrue(singleConsentShare.isFailure)
                state = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("locationShares")
                    .child(hostUid)
                    .get()
                    .await()
                    .getValue(LocationShareState::class.java)
                assertEquals(null, state?.latitude)
                assertEquals(null, state?.longitude)

                featureRepository.setLocationSharingEnabled(roomId, guestUid, true)
                featureRepository.shareCurrentLocation(
                    roomId = roomId,
                    uid = hostUid,
                    latitude = 37.5665,
                    longitude = 126.9780,
                    accuracyMeters = 24f,
                    approximateOnly = false,
                )
                state = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("locationShares")
                    .child(hostUid)
                    .get()
                    .await()
                    .getValue(LocationShareState::class.java)
                val sharedState = requireNotNull(state)
                assertEquals(37.5665, requireNotNull(sharedState.latitude), 0.0001)
                assertEquals(126.9780, requireNotNull(sharedState.longitude), 0.0001)
                assertEquals(false, sharedState.isApproximateOnly)
                assertEquals(mapOf(hostUid to true, guestUid to true), sharedState.consentedUids)

                featureRepository.setLocationSharingEnabled(roomId, hostUid, false)
                state = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("locationShares")
                    .child(hostUid)
                    .get()
                    .await()
                    .getValue(LocationShareState::class.java)
                assertEquals(false, state?.enabled)
                assertEquals(null, state?.latitude)
                assertEquals(null, state?.longitude)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun storesQuizAnswersAndDiscussionInRoom() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val questionId = "taste-1"
                val roomId = roomRepository.createRoom(hostUid, title = "퀴즈 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)

                featureRepository.answerQuiz(roomId, hostUid, questionId, "산책")
                featureRepository.answerQuiz(roomId, guestUid, questionId, "영화")
                featureRepository.addQuizDiscussion(roomId, hostUid, questionId, "둘 다 답했으니 산책하면서 영화 얘기하자")

                val answers = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("quizAnswers")
                    .get()
                    .await()
                    .children
                    .mapNotNull { it.getValue(QuizAnswer::class.java) }
                    .filter { it.questionId == questionId }
                val discussions = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("quizDiscussions")
                    .get()
                    .await()
                    .children
                    .mapNotNull { it.getValue(QuizDiscussion::class.java) }

                assertEquals(setOf(hostUid, guestUid), answers.map { it.uid }.toSet())
                assertEquals("둘 다 답했으니 산책하면서 영화 얘기하자", discussions.single().message)
                assertEquals(questionId, discussions.single().questionId)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    @Test
    fun uploadsQuizAnswerAndDailySparkDiscussionImagesToStorage() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(30_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val authResult = firebase.auth.signInAnonymously().await()
                val hostUid = requireNotNull(authResult.user?.uid)
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)
                val uploadFile = createTempUploadFile("quiz-${UUID.randomUUID()}.jpg")

                val roomId = roomRepository.createRoom(hostUid, title = "퀴즈 이미지 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                val questionId = "taste-image-${UUID.randomUUID()}"
                val dateKey = "2026-05-28"
                val dailySparkDiscussionId = ModelFactory.dailySparkDiscussionId(dateKey)

                featureRepository.answerQuiz(roomId, hostUid, questionId, "사진으로 남기기", Uri.fromFile(uploadFile))
                featureRepository.addQuizDiscussion(roomId, hostUid, dailySparkDiscussionId, "오늘 답변에 붙인 이미지", Uri.fromFile(uploadFile))

                val storedAnswer = requireNotNull(
                    firebase.root
                        .child("rooms")
                        .child(roomId)
                        .child("quizAnswers")
                        .child("$questionId-$hostUid")
                        .get()
                        .await()
                        .getValue(QuizAnswer::class.java),
                )
                val storedDiscussion = requireNotNull(
                    firebase.root
                        .child("rooms")
                        .child(roomId)
                        .child("quizDiscussions")
                        .get()
                        .await()
                        .children
                        .mapNotNull { it.getValue(QuizDiscussion::class.java) }
                        .single { it.questionId == dailySparkDiscussionId },
                )

                val answerPath = requireNotNull(storedAnswer.storagePath)
                val discussionPath = requireNotNull(storedDiscussion.storagePath)
                val answerBytes = firebase.storage.reference.child(answerPath).getBytes(1024).await()
                val discussionBytes = firebase.storage.reference.child(discussionPath).getBytes(1024).await()

                assertEquals(questionId, storedAnswer.questionId)
                assertTrue(storedAnswer.imageUrl?.isNotBlank() == true)
                assertTrue(answerPath.startsWith("rooms/$roomId/uploads/$hostUid/quizzes/"))
                assertEquals(1, answerBytes.size)
                assertEquals(dailySparkDiscussionId, storedDiscussion.questionId)
                assertTrue(storedDiscussion.imageUrl?.isNotBlank() == true)
                assertTrue(discussionPath.startsWith("rooms/$roomId/uploads/$hostUid/quizDiscussions/"))
                assertEquals(1, discussionBytes.size)

                uploadFile.delete()
                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.auth.signOut()
            }
        }
    }

    @Test
    fun storesDailySparkAnswersByRoomAndDate() {
        runBlocking {
            assumeTrue(BuildConfig.USE_FIREBASE_EMULATORS)

            withTimeout(20_000) {
                val firebase = FirebaseProvider()
                firebase.auth.signOut()
                val roomRepository = RoomRepository(firebase)
                val featureRepository = FeatureRepository(firebase)

                val hostUid = "host-${UUID.randomUUID()}"
                val guestUid = "guest-${UUID.randomUUID()}"
                val dateKey = "2026-05-28"
                val roomId = roomRepository.createRoom(hostUid, title = "Spark 테스트 방")
                val roomCode = requireNotNull(
                    firebase.root.child("rooms").child(roomId).child("roomCode").get().await().getValue(String::class.java),
                )
                assertTrue(roomRepository.joinRoom(roomCode, guestUid) is JoinRoomResult.Success)

                val spark = featureRepository.ensureDailySpark(roomId, dateKey)
                featureRepository.answerDailySpark(roomId, dateKey, hostUid, "고마웠던 점")
                featureRepository.answerDailySpark(roomId, dateKey, guestUid, "같이 웃은 순간")

                val stored = firebase.root
                    .child("rooms")
                    .child(roomId)
                    .child("dailySparks")
                    .child(dateKey)
                    .get()
                    .await()
                    .getValue(DailySpark::class.java)

                assertEquals(spark.question, stored?.question)
                assertEquals(setOf(hostUid, guestUid), stored?.answers?.keys)
                assertEquals("고마웠던 점", stored?.answers?.get(hostUid)?.answer)

                firebase.root.child("rooms").child(roomId).removeValue().await()
                firebase.root.child("roomCodes").child(roomCode).removeValue().await()
                firebase.root.child("userRooms").child(hostUid).removeValue().await()
                firebase.root.child("userRooms").child(guestUid).removeValue().await()
            }
        }
    }

    private fun createTempUploadFile(name: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.cacheDir, name).apply {
            writeBytes(byteArrayOf(42))
        }
    }
}
