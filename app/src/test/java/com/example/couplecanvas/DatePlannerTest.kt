package com.example.couplecanvas

import com.example.couplecanvas.data.model.DateMode
import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DatePlanStatus
import com.example.couplecanvas.data.model.DateVote
import com.example.couplecanvas.data.model.Room
import com.example.couplecanvas.util.DateIdeaGenerator
import com.example.couplecanvas.util.DatePlanMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatePlannerTest {
    @Test
    fun generatorCreatesThreeSafeLocalTonePlans() {
        val plans = DateIdeaGenerator.generate(
            uid = "me",
            mode = DateMode.Home,
            vibe = "Creative",
            budget = "낮음",
            time = "30분",
        )

        assertEquals(listOf("safe", "playful", "bold"), plans.map { it.tone })
        assertEquals(setOf("home"), plans.map { it.mode }.toSet())
        assertTrue(plans.all { it.estimatedBudget == "낮음" && it.estimatedTime == "30분" })
        assertTrue(plans.all { it.description.contains("외부 장소 검색 없이") })
        assertTrue(plans.all { it.steps.size == 3 })
    }

    @Test
    fun customPlanTrimsInputAndBecomesUpcomingWhenScheduled() {
        val plan = DateIdeaGenerator.customPlan(
            uid = "me",
            title = "  토요일 기록 데이트  ",
            description = "  같이 걷고 사진 한 장 남기기  ",
            mode = DateMode.Nearby,
            vibe = "  Walk & Talk  ",
            budget = "  낮음  ",
            time = "  1시간  ",
            stepsText = "만날 장소 확인\n\n사진 한 장 고르기\n후기 남기기",
            scheduledAt = 1_800_000_000_000L,
        )

        assertEquals("토요일 기록 데이트", plan.title)
        assertEquals("같이 걷고 사진 한 장 남기기", plan.description)
        assertEquals("nearby", plan.mode)
        assertEquals("Walk & Talk", plan.vibe)
        assertEquals("낮음", plan.estimatedBudget)
        assertEquals("1시간", plan.estimatedTime)
        assertEquals(DatePlanStatus.Upcoming.value, plan.status)
        assertEquals(listOf("만날 장소 확인", "사진 한 장 고르기", "후기 남기기"), plan.steps)
    }

    @Test
    fun customPlanFallsBackToSafeStepsAndSavedStatusWithoutDate() {
        val plan = DateIdeaGenerator.customPlan(
            uid = "me",
            title = "집 데이트",
            description = "간단히 쉬는 날",
            mode = DateMode.Home,
            vibe = "",
            budget = "",
            time = "",
            stepsText = "",
            scheduledAt = null,
        )

        assertEquals(DatePlanStatus.Saved.value, plan.status)
        assertEquals("Cozy", plan.vibe)
        assertEquals(null, plan.estimatedBudget)
        assertEquals(null, plan.estimatedTime)
        assertEquals(3, plan.steps.size)
    }

    @Test
    fun matcherRequiresTwoRoomMembersToLikeSamePlan() {
        val room = Room(members = mapOf("me" to true, "partner" to true))

        val oneLike = DatePlan(votes = mapOf("me" to DateVote.Like.value))
        val twoLikes = DatePlan(votes = mapOf("me" to DateVote.Like.value, "partner" to DateVote.Like.value))
        val outsiderLike = DatePlan(votes = mapOf("me" to DateVote.Like.value, "someoneElse" to DateVote.Like.value))
        val persistedMatch = DatePlan(matchedBy = mapOf("me" to true, "partner" to true))

        assertFalse(DatePlanMatcher.isMatched(oneLike, room))
        assertTrue(DatePlanMatcher.isMatched(twoLikes, room))
        assertFalse(DatePlanMatcher.isMatched(outsiderLike, room))
        assertTrue(DatePlanMatcher.isMatched(persistedMatch, null))
    }
}
