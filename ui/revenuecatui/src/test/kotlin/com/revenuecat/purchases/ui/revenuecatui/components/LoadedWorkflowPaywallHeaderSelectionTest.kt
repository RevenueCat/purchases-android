package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPendingTransition
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class LoadedWorkflowPaywallHeaderSelectionTest {

    private fun info(hasHeroImage: Boolean, hasHeader: Boolean) =
        WorkflowHeaderStepInfo(hasHeroImage = hasHeroImage, hasHeader = hasHeader)

    private fun transition(direction: NavigationDirection) =
        WorkflowPendingTransition("from", direction, 1)

    // --- present <-> absent: the cases the fade is for ---

    @Test
    fun `header to no-header fades the outgoing header out`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = true),
                "target" to info(hasHeroImage = false, hasHeader = false),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("from")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.LEAVING)
    }

    @Test
    fun `header to hero-without-header still fades the outgoing header out`() {
        // Behavior change: previously this dropped the header (hero rule). Now it fades.
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = true),
                "target" to info(hasHeroImage = true, hasHeader = false),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("from")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.LEAVING)
    }

    @Test
    fun `no-header to header fades the incoming header in`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = false),
                "target" to info(hasHeroImage = false, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.ENTERING)
    }

    @Test
    fun `missing outgoing hero header falls back to incoming header entering`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = true, hasHeader = false),
                "target" to info(hasHeroImage = false, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.ENTERING)
    }

    // --- both present: existing selection preserved, no fade ---

    @Test
    fun `hero to non-hero both-header keeps outgoing header stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = true, hasHeader = true),
                "target" to info(hasHeroImage = false, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("from")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    @Test
    fun `non-hero to hero both-header forward uses incoming header stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = true),
                "target" to info(hasHeroImage = true, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    @Test
    fun `non-hero to hero both-header backward uses incoming header stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = true),
                "target" to info(hasHeroImage = true, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.BACKWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    @Test
    fun `non-hero to non-hero both-header backward keeps outgoing header stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = true),
                "target" to info(hasHeroImage = false, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.BACKWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("from")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    @Test
    fun `non-hero to non-hero both-header forward uses incoming header stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = true),
                "target" to info(hasHeroImage = false, hasHeader = true),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    // --- degenerate cases ---

    @Test
    fun `no-header to no-header renders current step stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "from" to info(hasHeroImage = false, hasHeader = false),
                "target" to info(hasHeroImage = false, hasHeader = false),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    @Test
    fun `pending transition with unknown from step renders current step`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                // "from" is intentionally absent from the map
                "target" to info(hasHeroImage = false, hasHeader = false),
            ),
            pendingTransition = transition(NavigationDirection.FORWARD),
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }

    @Test
    fun `idle state uses current step header stable`() {
        val presentation = selectWorkflowHeaderPresentation(
            currentStepId = "target",
            stepInfoByStepId = mapOf(
                "target" to info(hasHeroImage = false, hasHeader = true),
            ),
            pendingTransition = null,
        )

        assertThat(presentation.headerStepId).isEqualTo("target")
        assertThat(presentation.role).isEqualTo(WorkflowHeaderTransitionRole.STABLE)
    }
}
