@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowStepGating
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateOutcome
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CheckpointWorkflowGateControllerTest {
    private val outcomes = mutableListOf<CheckpointGateOutcome>()
    private val controller = CheckpointWorkflowGateController()
    private val hard = step(WorkflowStepGating.Hard(setOf("pro", "premium")))

    @Test
    fun `hard gate without checkpoint context is disabled`() {
        controller.reset(null)
        assertThat(controller.isGated(hard)).isFalse()
        assertThat(controller.evaluate(hard, emptySet())).isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(controller.blocksForwardNavigation(hard)).isFalse()
    }

    @Test
    fun `only enforced gates need the customer's entitlements`() {
        reset()
        assertThat(controller.isGated(hard)).isTrue()
        assertThat(controller.isGated(step(WorkflowStepGating.Invalid))).isTrue()
        assertThat(controller.isGated(step(WorkflowStepGating.Soft))).isFalse()
        assertThat(controller.isGated(step(null))).isFalse()
    }

    @Test
    fun `any matching entitlement passes and reports once`() {
        reset()
        assertThat(controller.evaluate(hard, setOf("premium"))).isEqualTo(WorkflowGateDecision.PASSED)
        controller.evaluate(hard, setOf("premium"))
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.PASSED)
    }

    @Test
    fun `missing entitlement blocks forward navigation`() {
        reset()
        assertThat(controller.evaluate(hard, setOf("unrelated"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.blocksForwardNavigation(hard)).isTrue()
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `soft and absent gating do not block`() {
        reset()
        listOf(null, WorkflowStepGating.Soft).forEach { gating ->
            assertThat(controller.evaluate(step(gating), emptySet())).isEqualTo(WorkflowGateDecision.NOT_GATED)
        }
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE)
    }

    @Test
    fun `invalid gating fails closed even with active entitlements`() {
        reset()
        val invalid = step(WorkflowStepGating.Invalid)
        assertThat(controller.evaluate(invalid, setOf("pro"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.blocksForwardNavigation(invalid)).isTrue()
        assertThat(controller.update(customerInfo("pro", "premium"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `backtracking keeps unresolved outcome and reentry stays blocked`() {
        reset()
        controller.evaluate(hard, emptySet())
        val earlier = step(null)
        assertThat(controller.evaluate(earlier, emptySet())).isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(controller.blocksForwardNavigation(earlier)).isFalse()
        assertThat(controller.evaluate(hard, emptySet())).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `reentry reads fresh entitlements`() {
        reset()
        controller.evaluate(hard, emptySet())
        assertThat(controller.evaluate(hard, setOf("pro"))).isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(controller.hasUnresolvedGate).isFalse()
    }

    @Test
    fun `customer info update resolves a reached gate even while backtracking`() {
        reset()
        controller.evaluate(hard, emptySet())
        controller.evaluate(step(null), emptySet())
        assertThat(controller.update(customerInfo("unrelated"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.update(customerInfo("premium"))).isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(controller.hasUnresolvedGate).isFalse()
        assertThat(controller.blocksForwardNavigation(hard)).isFalse()
        assertThat(outcomes).containsExactly(
            CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED, CheckpointGateOutcome.PASSED,
        )
    }

    @Test
    fun `customer info update before reaching a gate does not report an outcome`() {
        reset()
        assertThat(controller.update(customerInfo("pro"))).isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE)
    }

    @Test
    fun `reset clears unresolved gate and old callback ownership`() {
        reset()
        controller.evaluate(hard, emptySet())
        val nextOutcomes = mutableListOf<CheckpointGateOutcome>()
        controller.reset(CheckpointGateContext(nextOutcomes::add))
        assertThat(controller.hasUnresolvedGate).isFalse()
        assertThat(controller.evaluate(hard, setOf("pro"))).isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
        assertThat(nextOutcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.PASSED)
    }

    private fun reset() = controller.reset(CheckpointGateContext(outcomes::add))

    private fun step(gating: WorkflowStepGating?) = WorkflowStep(
        id = if (gating is WorkflowStepGating.Hard) "hard" else "earlier",
        type = "screen",
        screenId = "screen",
        paramValues = when (gating) {
            is WorkflowStepGating.Hard -> mapOf("gating" to JsonObject(mapOf(
                "type" to JsonPrimitive("hard"),
                "entitlement_identifiers" to JsonArray(gating.entitlementIdentifiers.map(::JsonPrimitive)),
            )))
            WorkflowStepGating.Soft -> mapOf("gating" to JsonObject(mapOf("type" to JsonPrimitive("soft"))))
            WorkflowStepGating.Invalid -> mapOf("gating" to JsonNull)
            null -> emptyMap()
        },
    )

    private fun customerInfo(vararg ids: String): CustomerInfo = mockk {
        every { entitlements.active } returns ids.associateWith { mockk() }
    }
}
