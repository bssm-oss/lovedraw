package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.DateMode
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DateTone
import java.util.UUID

object DateIdeaGenerator {
    val vibes = listOf("Cozy", "Adventure", "Cute", "Chill", "Foodie", "Creative", "Movie Night", "Walk & Talk", "Long Distance Call")

    private val localTemplates = mapOf(
        "Cozy" to listOf("따뜻한 음료와 대화", "서로의 하루 플레이리스트 만들기", "짧은 편지 교환"),
        "Adventure" to listOf("동네 미니 탐험 루트 만들기", "새 산책길 30분 걷기", "안전한 새 운동 하나 체험"),
        "Cute" to listOf("서로 캐릭터 그려주기", "커플 별명 카드 만들기", "작은 선물 쪽지 숨기기"),
        "Chill" to listOf("느린 산책", "각자 좋아하는 장면 공유", "휴식 루틴 같이 정하기"),
        "Foodie" to listOf("집에서 테마 간식 만들기", "좋아하는 맛 월드컵", "같이 장보기 리스트 만들기"),
        "Creative" to listOf("공동 낙서 포스터 만들기", "사진 한 장으로 이야기 쓰기", "방 분위기 스케치"),
        "Movie Night" to listOf("영화 후보 3개 투표", "장면별 감상 노트", "엔딩 후 5분 리뷰"),
        "Walk & Talk" to listOf("질문 카드 산책", "서로 칭찬 3개 말하기", "다음 데이트 아이디어 걷기 회의"),
        "Long Distance Call" to listOf("같은 간식 먹으며 통화", "화면 공유 낙서", "각자 창밖 사진 공유"),
    )

    fun randomVibe(): String = vibes.random()

    fun generate(
        uid: String,
        mode: DateMode,
        vibe: String,
        budget: String,
        time: String,
    ): List<DatePlan> = listOf(
        createPlan(uid, mode, DateTone.Safe, vibe, budget, time),
        createPlan(uid, mode, DateTone.Playful, vibe, budget, time),
        createPlan(uid, mode, DateTone.Bold, vibe, budget, time),
    )

    fun customPlan(
        uid: String,
        title: String,
        description: String,
        mode: DateMode,
        vibe: String,
        budget: String?,
        time: String?,
        stepsText: String,
        scheduledAt: Long?,
    ): DatePlan {
        val trimmedTitle = title.trim()
        val trimmedDescription = description.trim()
        require(trimmedTitle.isNotEmpty()) { "데이트 제목을 입력해주세요" }
        require(trimmedDescription.isNotEmpty()) { "데이트 설명을 입력해주세요" }
        val steps = stepsText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty {
                listOf(
                    "서로 오늘 컨디션을 먼저 확인해요",
                    "무리 없는 속도로 같이 시간을 보내요",
                    "끝나고 짧은 후기를 남겨요",
                )
            }
        val now = System.currentTimeMillis()
        return DatePlan(
            planId = UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = trimmedDescription,
            vibe = vibe.trim().ifBlank { "Cozy" },
            mode = mode.value,
            tone = DateTone.Safe.value,
            estimatedTime = time?.trim()?.takeIf { it.isNotEmpty() },
            estimatedBudget = budget?.trim()?.takeIf { it.isNotEmpty() },
            steps = steps,
            scheduledAt = scheduledAt,
            status = if (scheduledAt == null) DatePlanStatus.Saved.value else DatePlanStatus.Upcoming.value,
            createdAt = now,
            createdByUid = uid,
            updatedAt = now,
        )
    }

    private fun createPlan(uid: String, mode: DateMode, tone: DateTone, vibe: String, budget: String, time: String): DatePlan {
        val toneName = when (tone) {
            DateTone.Safe -> "편안한"
            DateTone.Playful -> "장난스러운"
            DateTone.Bold -> "새로운 도전"
        }
        val modeName = when (mode) {
            DateMode.Nearby -> "근처"
            DateMode.LongDistance -> "장거리"
            DateMode.Home -> "집"
        }
        val candidates = localTemplates[vibe].orEmpty().ifEmpty { localTemplates.getValue("Cozy") }
        val anchor = candidates[tone.ordinal % candidates.size]
        val modeStep = when (mode) {
            DateMode.Nearby -> "실제 장소 추천 없이 가까운 곳에서 할 수 있게 동선을 짧게 잡아요"
            DateMode.LongDistance -> "영상통화나 사진 공유로 같은 경험처럼 맞춰요"
            DateMode.Home -> "집에서 바로 준비할 수 있는 물건만 사용해요"
        }
        val steps = when (tone) {
            DateTone.Safe -> listOf("서로 오늘 컨디션을 먼저 확인해요", anchor, "마지막에 한 줄 후기를 남겨요")
            DateTone.Playful -> listOf("가벼운 랜덤 미션 3개를 정해요", anchor, "가장 웃겼던 순간을 노트로 저장해요")
            DateTone.Bold -> listOf("무리하지 않는 선에서 새 루틴을 하나 시도해요", anchor, "다음에도 하고 싶은지 같이 투표해요")
        }
        return DatePlan(
            planId = UUID.randomUUID().toString(),
            title = "$toneName $modeName $vibe 데이트",
            description = "외부 장소 검색 없이 둘이 바로 실행할 수 있는 $time 플랜이에요. $modeStep",
            vibe = vibe,
            mode = mode.value,
            tone = tone.value,
            estimatedTime = time,
            estimatedBudget = budget,
            steps = steps,
            status = DatePlanStatus.Saved.value,
            createdAt = System.currentTimeMillis(),
            createdByUid = uid,
        )
    }
}
