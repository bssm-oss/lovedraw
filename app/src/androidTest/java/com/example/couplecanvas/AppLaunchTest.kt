package com.example.couplecanvas

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreenIsVisible() {
        composeRule.onNodeWithText("Couple Canvas").assertIsDisplayed()
    }

    @Test
    fun entryScreenIsReadyForOAuthOrExistingSession() {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            hasText("Google로 시작하기") || hasText("방 만들기")
        }

        if (hasText("Google로 시작하기")) {
            composeRule.onNodeWithText("Google로 시작하기")
                .assertIsDisplayed()
                .assertIsEnabled()

            composeRule
                .onAllNodesWithText("Firebase Console에서 Google 로그인 제공업체를 켜고 최신 google-services.json을 넣으면 실제 로그인이 동작합니다.")
                .assertCountEquals(0)
        } else {
            composeRule.onNodeWithText("방 만들기")
                .assertIsDisplayed()
                .assertIsEnabled()
            composeRule.onNodeWithText("코드로 입장")
                .assertIsDisplayed()
                .assertIsEnabled()
        }
    }

    private fun hasText(text: String): Boolean =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
}
