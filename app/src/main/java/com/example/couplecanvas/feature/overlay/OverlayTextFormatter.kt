package com.example.couplecanvas.feature.overlay

import com.example.couplecanvas.data.model.WidgetSnapshot

data class OverlayText(
    val title: String,
    val body: String,
    val helper: String,
)

object OverlayTextFormatter {
    private val defaultBodies = setOf(
        "함께한 날을 설정해요",
        "다음 데이트를 정해볼까요?",
        "새 노트가 있으면 여기에 보여요",
        "오늘의 질문을 열어보세요",
        "추억을 추가해보세요",
        "아직 통계가 없어요",
        "위치 공유가 꺼져 있어요",
    )

    fun format(snapshot: WidgetSnapshot): OverlayText {
        val title = snapshot.roomTitle.takeIf { it.isNotBlank() } ?: "Couple Canvas"
        if (snapshot.privacyMode) {
            return OverlayText(
                title = title,
                body = "새 기록이 있어요",
                helper = "앱에서 확인하기",
            )
        }

        val body = listOf(
            snapshot.latestNoteText,
            snapshot.nextDateText,
            snapshot.dailySparkText,
            snapshot.daysTogetherText,
            snapshot.latestMemoryTitle,
            snapshot.statsText,
            snapshot.distanceText,
        ).firstOrNull { it.isNotBlank() && it !in defaultBodies } ?: snapshot.daysTogetherText

        return OverlayText(
            title = title,
            body = body,
            helper = if (snapshot.roomId.isBlank()) "탭해서 열기" else "탭해서 방 열기",
        )
    }
}
