package com.example.couplecanvas.util

import com.example.couplecanvas.data.model.QuizQuestion

object QuizBank {
    fun defaultQuestions(): List<QuizQuestion> {
        val now = System.currentTimeMillis()
        return listOf(
            QuizQuestion("taste-1", "취향", "요즘 가장 같이 해보고 싶은 작은 일은?", listOf("산책", "영화", "요리", "낙서"), now),
            QuizQuestion("memory-1", "추억", "최근에 가장 오래 기억하고 싶은 순간은?", null, now),
            QuizQuestion("date-1", "데이트", "다음 데이트 분위기는 어떤 쪽이 좋아?", listOf("Cozy", "Creative", "Foodie", "Walk & Talk"), now),
            QuizQuestion("food-1", "음식", "같이 먹으면 기분이 좋아지는 메뉴는?", null, now),
            QuizQuestion("future-1", "미래 계획", "이번 달 둘이 같이 지키고 싶은 약속 하나는?", null, now),
        )
    }

    fun dailySparkQuestion(dateKey: String): String {
        val questions = listOf(
            "오늘 서로에게 고마웠던 점 하나는?",
            "이번 주 같이 만들고 싶은 작은 추억은?",
            "오늘의 기분을 색으로 표현하면?",
            "다음 만남에서 꼭 해보고 싶은 한 가지는?",
            "서로에게 보내고 싶은 짧은 응원은?",
        )
        return questions[Math.floorMod(dateKey.hashCode(), questions.size)]
    }
}
