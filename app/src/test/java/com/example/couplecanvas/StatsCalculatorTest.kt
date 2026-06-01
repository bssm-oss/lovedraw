package com.example.couplecanvas

import com.example.couplecanvas.data.model.DailySpark
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.SparkAnswer
import com.example.couplecanvas.util.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsCalculatorTest {
    @Test
    fun daysTogetherCountsStartDayAsOne() {
        val start = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(1, StatsCalculator.daysTogether(start, LocalDate.of(2026, 5, 1)))
        assertEquals(27, StatsCalculator.daysTogether(start, LocalDate.of(2026, 5, 27)))
    }

    @Test
    fun sparkCompletionRequiresEveryRoomMemberAnswer() {
        val spark = DailySpark(
            sparkId = "2026-05-28",
            dateKey = "2026-05-28",
            answers = mapOf("me" to SparkAnswer("me", "고마워", 1L)),
        )

        assertFalse(StatsCalculator.isSparkComplete(spark, setOf("me", "partner")))
        assertFalse(StatsCalculator.isSparkComplete(spark, setOf("me")))
    }

    @Test
    fun currentSparkStreakKeepsYesterdayStreakWhileTodayIsPending() {
        val members = setOf("me", "partner")
        val sparks = listOf(
            completedSpark("2026-05-26", members),
            completedSpark("2026-05-27", members),
            DailySpark("2026-05-28", "오늘 질문", "2026-05-28", answers = mapOf("me" to SparkAnswer("me", "아직 한 명", 3L))),
        )

        assertEquals(2, StatsCalculator.currentSparkStreak(sparks, members, LocalDate.of(2026, 5, 28)))
    }

    @Test
    fun currentSparkStreakIncludesTodayWhenBothAnswered() {
        val members = setOf("me", "partner")
        val sparks = listOf(
            completedSpark("2026-05-26", members),
            completedSpark("2026-05-27", members),
            completedSpark("2026-05-28", members),
        )

        assertTrue(StatsCalculator.isSparkComplete(sparks.last(), members))
        assertEquals(3, StatsCalculator.currentSparkStreak(sparks, members, LocalDate.of(2026, 5, 28)))
    }

    @Test
    fun nextDateCountdownUsesNearestUpcomingScheduledPlan() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 5, 28)
        val tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val nextWeek = today.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val plans = listOf(
            DatePlan(title = "다음 주 영화", status = DatePlanStatus.Upcoming.value, scheduledAt = nextWeek),
            DatePlan(title = "내일 산책", status = DatePlanStatus.Upcoming.value, scheduledAt = tomorrow),
            DatePlan(title = "저장만 한 플랜", status = DatePlanStatus.Saved.value, scheduledAt = tomorrow),
        )

        assertEquals("D-1 · 내일 산책", StatsCalculator.nextDateCountdownText(plans, today, zone))
    }

    @Test
    fun nextDateCountdownMarksTodayAsDDayAndPromptsForUnscheduledUpcoming() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 5, 28)
        val todayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(
            "D-Day · 오늘 전시",
            StatsCalculator.nextDateCountdownText(
                listOf(DatePlan(title = "오늘 전시", status = DatePlanStatus.Upcoming.value, scheduledAt = todayMillis)),
                today,
                zone,
            ),
        )
        assertEquals(
            "예정일을 설정해요 · 날짜 없는 플랜",
            StatsCalculator.nextDateCountdownText(
                listOf(DatePlan(title = "날짜 없는 플랜", status = DatePlanStatus.Upcoming.value)),
                today,
                zone,
            ),
        )
    }

    private fun completedSpark(dateKey: String, members: Set<String>): DailySpark =
        DailySpark(
            sparkId = dateKey,
            question = "질문",
            dateKey = dateKey,
            answers = members.associateWith { uid -> SparkAnswer(uid = uid, answer = "$uid 답변", createdAt = 1L) },
        )
}
