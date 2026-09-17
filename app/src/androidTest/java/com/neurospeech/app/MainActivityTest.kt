package com.neurospeech.app

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for the screen flow. Requires a device/emulator with the ARM64
 * native library support; the model preparation step may need network access.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun homeScreenShowsStartButton() {
        composeRule.onNodeWithText("Start Assessment").assertIsDisplayed()
    }

    @Test
    fun tappingStartLeavesHomeForPreparation() {
        composeRule.onNodeWithText("Start Assessment").performClick()
        // The preparation phase is brief when the model is already cached, so
        // accept either the Preparing message or the first assessment prompt.
        composeRule.waitUntil(timeoutMillis = 300_000) {
            composeRule.onAllNodesWithText("Preparing the speech engine…")
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Say the name of the picture, then tap Stop.")
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }
}