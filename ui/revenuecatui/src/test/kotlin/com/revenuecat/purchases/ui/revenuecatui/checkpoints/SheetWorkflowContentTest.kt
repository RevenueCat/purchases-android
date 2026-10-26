package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.composables.MODAL_SHEET_SCRIM_TEST_TAG
import com.revenuecat.purchases.ui.revenuecatui.composables.ModalSheetState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SheetWorkflowContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dismissing the sheet closes the paywall rather than backing out`(): Unit = with(composeTestRule) {
        val viewModel = mockk<PaywallViewModel>(relaxed = true)
        every { viewModel.state } returns MutableStateFlow(PaywallState.Loading)
        val options = PaywallOptions.Builder(dismissRequest = {}).build()
        setContent { SheetWorkflowContent(ModalSheetState(), options, viewModel) }
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        // The sheet covers the scrim except for the top margin, so the tap has to land there.
        onNodeWithTag(MODAL_SHEET_SCRIM_TEST_TAG).performTouchInput { click(Offset(centerX, 1f)) }

        verify(exactly = 1) { viewModel.closePaywall(null, PaywallDismissReason.CLOSE) }
    }

    private companion object {
        const val ANIMATION_SLACK_MILLIS = 1_000L
    }
}
