package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class LoadedWorkflowPaywallHeaderSelectionTest {

    @Test
    fun `hero to non-hero keeps outgoing hero header while animating`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = true, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = "from",
                animatingDirection = NavigationDirection.FORWARD,
                navigationDirection = NavigationDirection.FORWARD,
            ),
        )

        assertThat(selected).isEqualTo("from")
    }

    @Test
    fun `hero to non-hero switches to target header after animation`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = true, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = null,
                animatingDirection = NavigationDirection.NONE,
                navigationDirection = NavigationDirection.FORWARD,
            ),
        )

        assertThat(selected).isEqualTo("target")
    }

    @Test
    fun `non-hero to hero uses incoming header immediately`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = true, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = "from",
                animatingDirection = NavigationDirection.FORWARD,
                navigationDirection = NavigationDirection.FORWARD,
            ),
        )

        assertThat(selected).isEqualTo("target")
    }

    @Test
    fun `current step gap keeps outgoing header before animation state is bound`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "from",
                animatingFromStepId = null,
                animatingDirection = NavigationDirection.NONE,
                navigationDirection = NavigationDirection.BACKWARD,
            ),
        )

        assertThat(selected).isEqualTo("from")
    }

    @Test
    fun `non-hero to hero backward transition uses incoming header while animating`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = true, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = "from",
                animatingDirection = NavigationDirection.BACKWARD,
                navigationDirection = NavigationDirection.BACKWARD,
            ),
        )

        assertThat(selected).isEqualTo("target")
    }

    @Test
    fun `non-hero to non-hero backward transition keeps outgoing header while animating`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = "from",
                animatingDirection = NavigationDirection.BACKWARD,
                navigationDirection = NavigationDirection.BACKWARD,
            ),
        )

        assertThat(selected).isEqualTo("from")
    }

    @Test
    fun `non-hero to non-hero forward transition uses incoming header while animating`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = "from",
                animatingDirection = NavigationDirection.FORWARD,
                navigationDirection = NavigationDirection.FORWARD,
            ),
        )

        assertThat(selected).isEqualTo("target")
    }

    @Test
    fun `missing outgoing hero header falls back to target header`() {
        val selected = selectWorkflowHeaderStepId(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to WorkflowHeaderStepInfo(hasHeroImage = true, hasHeader = false),
                "target" to WorkflowHeaderStepInfo(hasHeroImage = false, hasHeader = true),
            ),
            transitionState = WorkflowHeaderTransitionState(
                seenStepId = "target",
                animatingFromStepId = "from",
                animatingDirection = NavigationDirection.FORWARD,
                navigationDirection = NavigationDirection.FORWARD,
            ),
        )

        assertThat(selected).isEqualTo("target")
    }
}
