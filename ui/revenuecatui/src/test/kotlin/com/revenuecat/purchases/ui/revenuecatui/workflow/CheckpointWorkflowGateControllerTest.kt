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
    private var appUserId = "customer-a"
    private val controller = CheckpointWorkflowGateController { appUserId }
    private val hard = step(WorkflowStepGating.Hard(setOf("pro", "premium")))

    @Test
    fun `hard gate without checkpoint context is disabled`() {
        controller.reset(null)
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(controller.blocksForwardNavigation(hard)).isFalse()
    }

    @Test
    fun `any initial matching entitlement passes and reports once`() {
        reset(setOf("premium"))
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.PASSED)
        controller.evaluate(hard)
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.PASSED)
    }

    @Test
    fun `missing initial entitlement blocks forward navigation`() {
        reset(setOf("unrelated"))
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.blocksForwardNavigation(hard)).isTrue()
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `unavailable initial customer info fails closed`() {
        reset(null)
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.BLOCKED)
    }

    @Test
    fun `soft and absent gating do not block`() {
        reset(emptySet())
        listOf(null, WorkflowStepGating.Soft).forEach { gating ->
            assertThat(controller.evaluate(step(gating))).isEqualTo(WorkflowGateDecision.NOT_GATED)
        }
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE)
    }

    @Test
    fun `invalid gating fails closed even with active entitlements`() {
        reset(setOf("pro"))
        val invalid = step(WorkflowStepGating.Invalid)
        assertThat(controller.evaluate(invalid)).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.blocksForwardNavigation(invalid)).isTrue()
        assertThat(controller.update(customerInfo("pro", "premium"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `backtracking keeps unresolved outcome and reentry stays blocked`() {
        reset(emptySet())
        controller.evaluate(hard)
        val earlier = step(null)
        assertThat(controller.evaluate(earlier)).isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(controller.blocksForwardNavigation(earlier)).isFalse()
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `customer info update resolves a reached gate even while backtracking`() {
        reset(emptySet())
        controller.evaluate(hard)
        controller.evaluate(step(null))
        assertThat(controller.update(customerInfo("unrelated"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.update(customerInfo("premium"))).isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(controller.hasUnresolvedGate).isFalse()
        assertThat(controller.blocksForwardNavigation(hard)).isFalse()
        assertThat(outcomes).containsExactly(
            CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED, CheckpointGateOutcome.PASSED,
        )
    }

    @Test
    fun `customer info before reaching a gate updates the entry baseline`() {
        reset(emptySet())
        assertThat(controller.update(customerInfo("pro"))).isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.PASSED)
    }

    @Test
    fun `identity change invalidates initial entitlements and keeps backtracked gate unresolved`() {
        reset(setOf("pro"))
        appUserId = "customer-b"
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.BLOCKED)
        controller.evaluate(step(null))
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(controller.blocksForwardNavigation(hard)).isTrue()
        assertThat(controller.resolveIdentity("customer-b", customerInfo("premium")))
            .isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(controller.hasUnresolvedGate).isFalse()
    }

    @Test
    fun `identity change fails the gate closed until the new user's info is resolved`() {
        reset(emptySet())
        controller.evaluate(hard)
        controller.update(customerInfo("pro"))
        appUserId = "customer-b"

        assertThat(controller.needsIdentityResolution()).isTrue()
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(controller.blocksForwardNavigation(hard)).isTrue()
    }

    @Test
    fun `identity change keeps passage when the new user holds the entitlement, as after an alias`() {
        reset(emptySet())
        controller.evaluate(hard)
        controller.update(customerInfo("pro"))
        appUserId = "customer-b"

        assertThat(controller.resolveIdentity("customer-b", customerInfo("pro")))
            .isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(controller.needsIdentityResolution()).isFalse()
        assertThat(controller.hasUnresolvedGate).isFalse()
        assertThat(controller.blocksForwardNavigation(hard)).isFalse()
        assertThat(outcomes).containsExactly(
            CheckpointGateOutcome.NOT_APPLICABLE,
            CheckpointGateOutcome.NOT_PASSED,
            CheckpointGateOutcome.PASSED,
            CheckpointGateOutcome.NOT_PASSED,
            CheckpointGateOutcome.PASSED,
        )
    }

    @Test
    fun `identity change revokes passage when the new user lacks the entitlement`() {
        reset(emptySet())
        controller.evaluate(hard)
        controller.update(customerInfo("pro"))
        appUserId = "customer-b"

        assertThat(controller.resolveIdentity("customer-b", customerInfo("unrelated")))
            .isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.needsIdentityResolution()).isFalse()
        assertThat(controller.hasUnresolvedGate).isTrue()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `customer info updates are ignored while the identity is unresolved`() {
        reset(emptySet())
        controller.evaluate(hard)
        appUserId = "customer-b"

        assertThat(controller.update(customerInfo("pro"))).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.needsIdentityResolution()).isTrue()
        assertThat(controller.hasUnresolvedGate).isTrue()
    }

    @Test
    fun `resolution for an outdated user is ignored and a later one resolves`() {
        reset(emptySet())
        controller.evaluate(hard)
        appUserId = "customer-b"
        controller.needsIdentityResolution()
        appUserId = "customer-c"

        assertThat(controller.resolveIdentity("customer-b", customerInfo("pro")))
            .isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.needsIdentityResolution()).isTrue()
        assertThat(controller.resolveIdentity("customer-c", customerInfo("pro")))
            .isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(controller.needsIdentityResolution()).isFalse()
    }

    @Test
    fun `resolution before a gate is reached sets the entry baseline for the new user`() {
        reset(setOf("pro"))
        appUserId = "customer-b"
        assertThat(controller.resolveIdentity("customer-b", customerInfo("premium")))
            .isEqualTo(WorkflowGateDecision.NOT_GATED)
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.PASSED)
    }

    @Test
    fun `an unknown initial user id requires identity resolution`() {
        controller.reset(CheckpointGateContext(null, setOf("pro"), outcomes::add))
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.BLOCKED)
        assertThat(controller.needsIdentityResolution()).isTrue()
    }

    @Test
    fun `reset clears unresolved gate and old callback ownership`() {
        reset(emptySet())
        controller.evaluate(hard)
        val nextOutcomes = mutableListOf<CheckpointGateOutcome>()
        controller.reset(CheckpointGateContext("customer-a", setOf("pro"), nextOutcomes::add))
        assertThat(controller.hasUnresolvedGate).isFalse()
        assertThat(controller.evaluate(hard)).isEqualTo(WorkflowGateDecision.PASSED)
        assertThat(outcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.NOT_PASSED)
        assertThat(nextOutcomes).containsExactly(CheckpointGateOutcome.NOT_APPLICABLE, CheckpointGateOutcome.PASSED)
    }

    private fun reset(ids: Set<String>?) = controller.reset(CheckpointGateContext("customer-a", ids, outcomes::add))

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
