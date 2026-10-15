@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowStepGating
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WorkflowGateEvaluatorTest {

    private val hardGate = WorkflowStepGating.Hard(setOf("pro", "premium"))

    @Test
    fun `hard gate is satisfied by any configured entitlement`() {
        assertThat(hardGate.isSatisfiedBy(setOf("premium"))).isTrue()
    }

    @Test
    fun `hard gate is not satisfied by unrelated entitlements`() {
        assertThat(hardGate.isSatisfiedBy(setOf("unrelated"))).isFalse()
    }

    @Test
    fun `hard gate is not satisfied without active entitlements`() {
        assertThat(hardGate.isSatisfiedBy(emptySet())).isFalse()
    }

    @Test
    fun `soft gate is satisfied without active entitlements`() {
        assertThat(WorkflowStepGating.Soft.isSatisfiedBy(emptySet())).isTrue()
    }

    @Test
    fun `invalid gate fails open without active entitlements`() {
        assertThat(WorkflowStepGating.Invalid.isSatisfiedBy(emptySet())).isTrue()
    }

    @Test
    fun `absent gate is satisfied without active entitlements`() {
        assertThat((null as WorkflowStepGating?).isSatisfiedBy(emptySet())).isTrue()
    }
}
