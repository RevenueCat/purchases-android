@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * `copy` is how the paywall changes mode, and `hashCode` keys the ViewModel, so a field either of them
 * drops is silently lost for the whole presentation.
 */
class PaywallOptionsInjectedWorkflowTest {

    private val branch = WorkflowTriggerAction.Branch(
        branches = listOf(WorkflowTriggerAction.Branch.Route(audienceId = "aud", stepId = "routed")),
        fallbackStepId = "fallback",
    )

    private val resolvedBranchSteps = mapOf(branch to "routed")

    @Test
    fun `copy keeps the resolved branch routes`() {
        assertThat(options().copy().injectedWorkflowResolvedBranchSteps).isEqualTo(resolvedBranchSteps)
    }

    @Test
    fun `options differing only in their resolved branch routes are not equal`() {
        val other = options(resolved = mapOf(branch to "fallback"))

        assertThat(options()).isNotEqualTo(other)
        assertThat(options().hashCode()).isNotEqualTo(other.hashCode())
    }

    private fun options(
        resolved: Map<WorkflowTriggerAction.Branch, String> = resolvedBranchSteps,
    ): PaywallOptions = PaywallOptions.Builder(dismissRequest = {})
        .injectedWorkflow(
            workflow(),
            Offerings(current = null, all = emptyMap()),
            emptyUiConfig(),
            traceId = null,
            resolvedBranchSteps = resolved,
        )
        .build()

    private fun workflow() = PublishedWorkflow(
        id = "wf",
        displayName = "wf",
        initialStepId = "step-1",
        steps = emptyMap(),
        screens = emptyMap(),
    )
}
