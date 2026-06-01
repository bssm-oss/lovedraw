package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DailySpark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object StatsCalculator {
    fun daysTogether(startedAtMillis: Long?, now: LocalDate = LocalDate.now()): Int {
        if (startedAtMillis == null || startedAtMillis <= 0L) return 0
        val start = Instant.ofEpochMilli(startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(start, now).toInt().coerceAtLeast(0) + 1
    }

    fun monthlyDateCount(plans: List<DatePlan>, now: LocalDate = LocalDate.now()): Int =
        plans.count {
            it.status == DatePlanStatus.Past.value &&
                it.scheduledAt?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    date.year == now.year && date.month == now.month
                } == true
        }

    fun upcomingDateCount(plans: List<DatePlan>, nowMillis: Long = System.currentTimeMillis()): Int =
        plans.count { it.status == DatePlanStatus.Upcoming.value && (it.scheduledAt ?: Long.MAX_VALUE) >= nowMillis }

    fun nextDateCountdownText(
        plans: List<DatePlan>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val nextDatedPlan = plans
            .asSequence()
            .filter { it.status == DatePlanStatus.Upcoming.value && it.scheduledAt != null }
            .map { plan ->
                val date = Instant.ofEpochMilli(requireNotNull(plan.scheduledAt)).atZone(zoneId).toLocalDate()
                plan to date
            }
            .filter { (_, date) -> !date.isBefore(today) }
            .minByOrNull { (_, date) -> date }

        if (nextDatedPlan == null) {
            val unscheduled = plans.firstOrNull { it.status == DatePlanStatus.Upcoming.value && it.scheduledAt == null }
            return unscheduled?.let { "예정일을 설정해요 · ${it.title}" } ?: "다음 데이트를 정해볼까요?"
        }

        val (plan, date) = nextDatedPlan
        val daysLeft = ChronoUnit.DAYS.between(today, date).toInt()
        val prefix = if (daysLeft == 0) "D-Day" else "D-$daysLeft"
        return "$prefix · ${plan.title}"
    }

    fun currentSparkStreak(
        sparks: List<DailySpark>,
        memberIds: Set<String>,
        today: LocalDate = LocalDate.now(),
    ): Int {
        if (memberIds.size < 2) return 0
        val byDate = sparks.associateBy { it.dateKey }
        var cursor = if (isSparkComplete(byDate[today.toString()], memberIds)) today else today.minusDays(1)
        var streak = 0
        while (isSparkComplete(byDate[cursor.toString()], memberIds)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun isSparkComplete(spark: DailySpark?, memberIds: Set<String>): Boolean {
        if (spark == null || memberIds.size < 2) return false
        return memberIds.all { uid -> spark.answers[uid]?.answer?.isNotBlank() == true }
    }
}
