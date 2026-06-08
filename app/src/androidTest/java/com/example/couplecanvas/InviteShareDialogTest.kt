package com.example.couplecanvas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.couplecanvas.presentation.component.InviteShareDialog
import com.example.couplecanvas.presentation.theme.CoupleCanvasTheme
import org.junit.Rule
import org.junit.Test

class InviteShareDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inviteDialogExposesQrSocialShareAndCopyActions() {
        composeRule.setContent {
            CoupleCanvasTheme {
                InviteShareDialog(
                    roomCode = "ABCD23",
                    roomTitle = "우리 방",
                    onDismiss = {},
                    onMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("초대하기").assertIsDisplayed()
        composeRule.onNodeWithText("ABCD23").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("초대 QR 코드").assertIsDisplayed()
        composeRule.onNodeWithText("카톡").assertIsDisplayed()
        composeRule.onNodeWithText("인스타 DM").assertIsDisplayed()
        composeRule.onNodeWithText("공유").assertIsDisplayed()
        composeRule.onNodeWithText("링크 복사").assertIsDisplayed()
        composeRule.onNodeWithText("코드").assertIsDisplayed()
    }
}
