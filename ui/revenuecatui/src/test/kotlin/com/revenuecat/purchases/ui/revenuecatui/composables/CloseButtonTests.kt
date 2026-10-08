package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloseButtonTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `close button remains enabled while an action is in progress`() {
        var clicked = false
        composeTestRule.setContent {
            Box {
                CloseButton(
                    shouldDisplayDismissButton = true,
                    color = null,
                    actionInProgress = true,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule.onNode(hasClickAction())
            .assertIsEnabled()
            .performClick()

        assertThat(clicked).isTrue()
    }
}
