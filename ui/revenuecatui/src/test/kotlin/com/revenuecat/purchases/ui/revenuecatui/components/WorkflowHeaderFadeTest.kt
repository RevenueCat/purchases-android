package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A header that outlives its transition must not receive touches: at alpha 0 a node stays
 * hit-testable while being invisible to both screenshots and the accessibility tree.
 */
@RunWith(AndroidJUnit4::class)
internal class WorkflowHeaderFadeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun transitionStateAt(progress: Float) = WorkflowTransitionState.SlideInOut(
        animatingFromStepId = "from",
        animatingToStepId = "to",
        animatingDirection = NavigationDirection.FORWARD,
        animatable = Animatable(progress),
    )

    private fun clickThroughHeader(
        role: WorkflowHeaderTransitionRole,
        progress: Float,
    ): Pair<Int, Int> {
        var headerClicks = 0
        var stepClicks = 0
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().clickable { stepClicks++ })
                Box(
                    Modifier
                        .fillMaxSize()
                        .workflowHeaderFade(role, transitionStateAt(progress))
                        .clickable { headerClicks++ },
                )
            }
        }
        composeTestRule.onRoot().performTouchInput { click() }
        composeTestRule.waitForIdle()
        return headerClicks to stepClicks
    }

    @Test
    fun `fully faded leaving header does not intercept touches`() {
        val (headerClicks, stepClicks) = clickThroughHeader(
            role = WorkflowHeaderTransitionRole.LEAVING,
            progress = 1f,
        )

        assertThat(headerClicks).isZero()
        assertThat(stepClicks).isOne()
    }

    @Test
    fun `partially faded leaving header still intercepts touches`() {
        val (headerClicks, stepClicks) = clickThroughHeader(
            role = WorkflowHeaderTransitionRole.LEAVING,
            progress = 0.5f,
        )

        assertThat(headerClicks).isOne()
        assertThat(stepClicks).isZero()
    }

    @Test
    fun `stable header intercepts touches`() {
        val (headerClicks, stepClicks) = clickThroughHeader(
            role = WorkflowHeaderTransitionRole.STABLE,
            progress = 1f,
        )

        assertThat(headerClicks).isOne()
        assertThat(stepClicks).isZero()
    }

    @Test
    fun `entering header at the start of its fade does not intercept touches`() {
        val (headerClicks, stepClicks) = clickThroughHeader(
            role = WorkflowHeaderTransitionRole.ENTERING,
            progress = 0f,
        )

        assertThat(headerClicks).isZero()
        assertThat(stepClicks).isOne()
    }
}
