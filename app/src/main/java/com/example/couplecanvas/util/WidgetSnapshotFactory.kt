package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.RoomHomeSummary
import com.example.couplecanvas.data.model.RoomStatus
import com.example.couplecanvas.data.model.WidgetSnapshot

object WidgetSnapshotFactory {
    fun fromHomeSummary(summary: RoomHomeSummary, current: WidgetSnapshot = WidgetSnapshot()): WidgetSnapshot {
        val room = summary.room
        val keepCurrentDetails = current.roomId == room.roomId
        val daysTogether = StatsCalculator.daysTogether(room.startedAt)
        return WidgetSnapshot(
            roomId = room.roomId,
            roomTitle = room.title,
            privacyMode = room.privacyMode,
            daysTogetherText = daysTogetherText(daysTogether),
            nextDateText = preservedOrCount(
                keep = keepCurrentDetails && summary.datePlanCount > 0,
                current = current.nextDateText,
                fallback = if (summary.datePlanCount > 0) "데이트 플랜 ${summary.datePlanCount}개" else "다음 데이트를 정해볼까요?",
                defaultValue = WidgetSnapshot().nextDateText,
            ),
            latestNoteText = preservedOrCount(
                keep = keepCurrentDetails && summary.noteCount > 0,
                current = current.latestNoteText,
                fallback = noteText(summary),
                defaultValue = WidgetSnapshot().latestNoteText,
            ),
            dailySparkText = preservedOrCount(
                keep = keepCurrentDetails && summary.dailySparkCount > 0,
                current = current.dailySparkText,
                fallback = if (summary.dailySparkCount > 0) "오늘의 질문을 확인해요" else "오늘의 질문을 열어보세요",
                defaultValue = WidgetSnapshot().dailySparkText,
            ),
            latestMemoryTitle = preservedOrCount(
                keep = keepCurrentDetails && summary.memoryCount > 0,
                current = current.latestMemoryTitle,
                fallback = if (summary.memoryCount > 0) "추억 ${summary.memoryCount}개" else "추억 없음",
                defaultValue = WidgetSnapshot().latestMemoryTitle,
            ),
            latestMemoryId = current.latestMemoryId.takeIf { keepCurrentDetails && summary.memoryCount > 0 },
            latestMemoryImageUrl = current.latestMemoryImageUrl.takeIf { keepCurrentDetails && summary.memoryCount > 0 },
            latestMemoryLocalPath = current.latestMemoryLocalPath.takeIf { keepCurrentDetails && summary.memoryCount > 0 },
            latestDrawingUrl = current.latestDrawingUrl.takeIf { keepCurrentDetails && summary.drawingSnapshotCount > 0 },
            latestDrawingLocalPath = current.latestDrawingLocalPath.takeIf { keepCurrentDetails && summary.drawingSnapshotCount > 0 },
            latestDrawingText = if (summary.drawingSnapshotCount > 0) {
                preservedOrCount(
                    keep = keepCurrentDetails,
                    current = current.latestDrawingText,
                    fallback = "낙서 ${summary.drawingSnapshotCount}개",
                    defaultValue = WidgetSnapshot().latestDrawingText,
                )
            } else {
                "낙서 없음"
            },
            statsText = statsText(summary),
            distanceText = current.distanceText,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun daysTogetherText(daysTogether: Int): String =
        if (daysTogether > 0) "우리 함께한 지 ${daysTogether}일" else "사귄 날짜를 설정해요"

    private fun noteText(summary: RoomHomeSummary): String =
        when {
            summary.unreadNoteCount > 0 -> "새 노트 ${summary.unreadNoteCount}개"
            summary.noteCount > 0 -> "노트 ${summary.noteCount}개"
            else -> "노트 없음"
        }

    private fun statsText(summary: RoomHomeSummary): String {
        val room = summary.room
        val roomState = when (room.status) {
            RoomStatus.Closed.value -> "보관"
            RoomStatus.Waiting.value -> "대기"
            else -> "연결"
        }
        return "$roomState · 추억 ${summary.memoryCount} · 낙서 ${summary.drawingSnapshotCount} · 버킷 ${summary.bucketItemCount}"
    }

    private fun preservedOrCount(
        keep: Boolean,
        current: String,
        fallback: String,
        defaultValue: String,
    ): String =
        if (keep && current.isNotBlank() && current != defaultValue) current else fallback
}
