package com.example.couplecanvas.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

data class AppUser(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = 0L,
    val lastSeen: Long = 0L,
)

@IgnoreExtraProperties
data class Room(
    val roomId: String = "",
    val roomCode: String = "",
    val title: String = "둘만의 그림방",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val status: String = RoomStatus.Waiting.value,
    val hostUid: String = "",
    val guestUid: String? = null,
    val activeUserCount: Int = 0,
    val members: Map<String, Boolean> = emptyMap(),
    val startedAt: Long? = null,
    val privacyMode: Boolean = false,
)

data class RoomHomeSummary(
    val room: Room = Room(),
    val noteCount: Int = 0,
    val unreadNoteCount: Int = 0,
    val datePlanCount: Int = 0,
    val memoryCount: Int = 0,
    val drawingSnapshotCount: Int = 0,
    val dailySparkCount: Int = 0,
    val bucketItemCount: Int = 0,
)

enum class RoomStatus(val value: String) {
    Waiting("waiting"),
    Active("active"),
    Closed("closed"),
}

data class RoomUser(
    val uid: String = "",
    val role: String = "guest",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val joinedAt: Long = 0L,
    val online: Boolean = false,
    val lastSeen: Long = 0L,
)

data class DrawingPoint(
    val x: Float = 0f,
    val y: Float = 0f,
    val t: Long = 0L,
)

data class Stroke(
    val strokeId: String = "",
    val ownerUid: String = "",
    val color: String = "#D9FF5A6D",
    val width: Float = 15f,
    val eraser: Boolean = false,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val points: Map<String, DrawingPoint> = emptyMap(),
) {
    fun sortedPoints(): List<DrawingPoint> = points.toList().sortedBy { it.first }.map { it.second }

    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAt > 0L && expiresAt <= nowMillis

    fun alpha(nowMillis: Long = System.currentTimeMillis()): Float {
        if (expiresAt <= 0L) return 1f
        val remaining = expiresAt - nowMillis
        if (remaining >= LASER_FADE_MS) return 1f
        val progress = (remaining.toFloat() / LASER_FADE_MS.toFloat()).coerceIn(0f, 1f)
        return progress * progress * (3f - 2f * progress)
    }

    companion object {
        const val MARKER_RED = "#CFFF5A72"
        const val MARKER_BLUE = "#BD4E7DFF"
        const val MARKER_BLACK = "#A8272624"
        const val LASER_TTL_MS = 8_000L
        const val LASER_FADE_MS = 1_800L
    }
}

data class BrushState(
    val color: String = Stroke.MARKER_RED,
    val width: Float = 15f,
    val eraser: Boolean = false,
    val laser: Boolean = true,
)

data class DrawingUiState(
    val roomId: String = "",
    val strokes: List<Stroke> = emptyList(),
    val localPendingStrokes: List<Stroke> = emptyList(),
    val activeStrokes: List<Stroke> = emptyList(),
    val snapshots: List<DrawingSnapshot> = emptyList(),
    val emojis: List<CanvasEmoji> = emptyList(),
    val background: DrawingBackground? = null,
    val localActiveStroke: Stroke? = null,
    val brush: BrushState = BrushState(),
    val isConnected: Boolean = false,
    val partnerOnline: Boolean = false,
    val isLoading: Boolean = true,
    val isSavingSnapshot: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val nowMillis: Long = System.currentTimeMillis(),
)

data class CanvasEmoji(
    val emojiId: String = "",
    val ownerUid: String = "",
    val emoji: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val size: Float = 44f,
    val createdAt: Long = 0L,
)

data class DrawingBackground(
    val imageUrl: String = "",
    val storagePath: String = "",
    val ownerUid: String = "",
    val updatedAt: Long = 0L,
)

data class RoomUiState(
    val rooms: List<Room> = emptyList(),
    val selectedRoom: Room? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdRoomId: String? = null,
    val joinedRoomId: String? = null,
)

sealed interface JoinRoomResult {
    data class Success(val roomId: String) : JoinRoomResult
    data object NotFound : JoinRoomResult
    data object Full : JoinRoomResult
    data object Closed : JoinRoomResult
    data object AlreadyMember : JoinRoomResult
    data class Error(val message: String) : JoinRoomResult
}

data class CoupleProfile(
    val coupleId: String = "",
    val localUid: String = "",
    val partnerUid: String? = null,
    val startedAt: Long = 0L,
    val nickname: String? = null,
    val partnerNickname: String? = null,
    val privacyMode: Boolean = false,
)

data class CoupleStats(
    val daysTogether: Int = 0,
    val currentSparkStreak: Int = 0,
    val monthlyDateCount: Int = 0,
    val upcomingDateCount: Int = 0,
    val lastSyncedAt: Long? = null,
    val unreadNoteCount: Int = 0,
    val recentDrawingCount: Int = 0,
)

data class LoveNote(
    val noteId: String = "",
    val authorUid: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val updatedAt: Long = createdAt,
)

data class MemoryItem(
    val memoryId: String = "",
    val title: String = "",
    val note: String? = null,
    val imageUrls: List<String> = emptyList(),
    val storagePaths: List<String> = emptyList(),
    val date: Long = 0L,
    val createdByUid: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = createdAt,
)

data class BucketItem(
    val itemId: String = "",
    val title: String = "",
    val description: String? = null,
    val vibe: String? = null,
    val plannedDatePlanId: String? = null,
    val status: String = BucketStatus.Wish.value,
    val createdByUid: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class BucketStatus(val value: String) {
    Wish("wish"),
    Planned("planned"),
    Done("done"),
}

data class DatePlan(
    val planId: String = "",
    val title: String = "",
    val description: String = "",
    val vibe: String = "Cozy",
    val mode: String = DateMode.Nearby.value,
    val tone: String = DateTone.Safe.value,
    val estimatedTime: String? = null,
    val estimatedBudget: String? = null,
    val steps: List<String> = emptyList(),
    val scheduledAt: Long? = null,
    val status: String = DatePlanStatus.Saved.value,
    val createdAt: Long = 0L,
    val createdByUid: String = "",
    val votes: Map<String, String> = emptyMap(),
    val matchedAt: Long? = null,
    val matchedBy: Map<String, Boolean> = emptyMap(),
    val updatedAt: Long = createdAt,
)

enum class DateMode(val value: String) {
    Nearby("nearby"),
    LongDistance("long_distance"),
    Home("home"),
}

enum class DateTone(val value: String) {
    Safe("safe"),
    Playful("playful"),
    Bold("bold"),
}

enum class DatePlanStatus(val value: String) {
    Upcoming("upcoming"),
    Saved("saved"),
    Past("past"),
}

enum class DateVote(val value: String) {
    Like("like"),
    Nope("nope"),
    Later("later");

    companion object {
        fun from(value: String): DateVote? = entries.firstOrNull { it.value == value }
    }
}

data class QuizQuestion(
    val questionId: String = "",
    val category: String = "",
    val question: String = "",
    val options: List<String>? = null,
    val createdAt: Long = 0L,
)

data class QuizAnswer(
    val answerId: String = "",
    val questionId: String = "",
    val uid: String = "",
    val answer: String = "",
    val imageUrl: String? = null,
    val storagePath: String? = null,
    val createdAt: Long = 0L,
)

data class QuizDiscussion(
    val discussionId: String = "",
    val questionId: String = "",
    val authorUid: String = "",
    val message: String = "",
    val imageUrl: String? = null,
    val storagePath: String? = null,
    val createdAt: Long = 0L,
)

data class DailySpark(
    val sparkId: String = "",
    val question: String = "",
    val dateKey: String = "",
    val answers: Map<String, SparkAnswer> = emptyMap(),
)

data class SparkAnswer(
    val uid: String = "",
    val answer: String = "",
    val createdAt: Long = 0L,
)

data class DrawingSnapshot(
    val drawingId: String = "",
    val authorUid: String = "",
    val imageUrl: String = "",
    val storagePath: String = "",
    val createdAt: Long = 0L,
    val caption: String? = null,
)

data class LocationShareState(
    val uid: String = "",
    val enabled: Boolean = false,
    val consentedUids: Map<String, Boolean> = emptyMap(),
    val lastSharedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    @get:PropertyName("isApproximateOnly")
    @set:PropertyName("isApproximateOnly")
    var isApproximateOnly: Boolean = true,
)

data class SyncEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val uid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val type: String = "",
    val payload: Map<String, Any?> = emptyMap(),
)

data class WidgetSnapshot(
    val roomId: String = "",
    val roomTitle: String = "",
    val privacyMode: Boolean = false,
    val daysTogetherText: String = "함께한 날을 설정해요",
    val nextDateText: String = "다음 데이트를 정해볼까요?",
    val latestNoteText: String = "새 노트가 있으면 여기에 보여요",
    val dailySparkText: String = "오늘의 질문을 열어보세요",
    val latestMemoryTitle: String = "추억을 추가해보세요",
    val latestMemoryId: String? = null,
    val latestMemoryImageUrl: String? = null,
    val latestMemoryLocalPath: String? = null,
    val latestDrawingUrl: String? = null,
    val latestDrawingLocalPath: String? = null,
    val latestDrawingText: String = "낙서 없음",
    val statsText: String = "아직 통계가 없어요",
    val distanceText: String = "위치 공유가 꺼져 있어요",
    val updatedAt: Long = 0L,
)

object ModelFactory {
    fun dateKey(now: Instant = Instant.now(), zoneId: ZoneId = ZoneOffset.UTC): String =
        now.atZone(zoneId).toLocalDate().toString()

    fun dateKey(now: LocalDate): String =
        now.toString()

    fun dailySparkDiscussionId(dateKey: String): String =
        "dailySpark:$dateKey"
}
