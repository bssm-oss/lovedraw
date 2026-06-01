package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.DatePlan
import com.example.couplecanvas.data.model.DateVote
import com.example.couplecanvas.data.model.Room

object DatePlanMatcher {
    fun likedByMemberCount(plan: DatePlan, room: Room?): Int {
        val memberIds = room?.members?.filterValues { it }?.keys.orEmpty()
        return plan.votes.count { (uid, vote) -> uid in memberIds && vote == DateVote.Like.value }
    }

    fun isMatched(plan: DatePlan, room: Room?): Boolean =
        likedByMemberCount(plan, room) >= REQUIRED_MATCH_LIKES ||
            plan.matchedBy.count { it.value } >= REQUIRED_MATCH_LIKES

    const val REQUIRED_MATCH_LIKES: Int = 2
}
