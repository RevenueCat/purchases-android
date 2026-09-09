package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FlowResultMappingTest {

    @Test
    fun `a run that presented nothing has no result`() {
        val run = CheckpointRun(flowOutcome = null, backedOut = false)

        assertThat(run.toResult(activeEntitlementsBefore = null)).isNull()
    }

    @Test
    fun `a purchase obtains the entitlements that were not active before`() {
        val run = presented(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro", "plus"), mockk()))

        assertThat(run.toResult(setOf("plus")).obtainedIdentifiers()).containsExactly("pro")
    }

    @Test
    fun `without a snapshot every active entitlement counts as obtained`() {
        val run = presented(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro", "extra"), mockk()))

        assertThat(run.toResult(activeEntitlementsBefore = null).obtainedIdentifiers())
            .containsExactlyInAnyOrder("extra", "pro")
    }

    @Test
    fun `an obtained entitlement carries the full entitlement info`() {
        val customerInfo = customerInfoWithActive("pro")
        val run = presented(CheckpointFlowOutcome.Purchased(customerInfo, mockk()))

        val obtained = run.toResult(emptySet())?.obtainedEntitlements?.single()
        assertThat(obtained?.entitlementInfo).isSameAs(customerInfo.entitlements.active.getValue("pro"))
    }

    @Test
    fun `a restore obtains the entitlements that were not active before`() {
        val run = presented(CheckpointFlowOutcome.Restored(customerInfoWithActive("pro")))

        assertThat(run.toResult(emptySet()).obtainedIdentifiers()).containsExactly("pro")
    }

    @Test
    fun `a finished app-owned presentation obtains the entitlements that were not active before`() {
        val customerInfo = customerInfoWithActive("pro", "plus")
        val run = presented(CheckpointFlowOutcome.Finished(customerInfo, reportedPurchase = false))

        assertThat(run.toResult(setOf("plus")).obtainedIdentifiers()).containsExactly("pro")
    }

    @Test
    fun `a dismissed or web checkout outcome yields an empty result`() {
        listOf(CheckpointFlowOutcome.Dismissed, CheckpointFlowOutcome.WebCheckoutOpened).forEach { outcome ->
            assertThat(presented(outcome).toResult(emptySet())).isEqualTo(FlowResult(emptySet()))
        }
    }

    @Test
    fun `an in-flow error yields no result`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Simulated.")
        val run = presented(CheckpointFlowOutcome.Error(error))

        assertThat(run.toResult(emptySet())).isNull()
    }

    @Test
    fun `a failure before any flow reported yields no result`() {
        val run = CheckpointRun(flowOutcome = null, backedOut = false)

        assertThat(run.toResult(emptySet())).isNull()
    }

    @Test
    fun `an unknown flow outcome yields an empty result`() {
        val unknownOutcome = object : CheckpointFlowOutcome() {}

        assertThat(presented(unknownOutcome).toResult(emptySet())).isEqualTo(FlowResult(emptySet()))
    }

    private fun presented(outcome: CheckpointFlowOutcome) =
        CheckpointRun(outcome, backedOut = false)

    private fun FlowResult?.obtainedIdentifiers(): List<String>? =
        this?.obtainedEntitlements?.map { it.entitlementInfo.identifier }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo {
        val active = identifiers.associateWith { id -> mockk<EntitlementInfo> { every { identifier } returns id } }
        return mockk { every { entitlements.active } returns active }
    }
}
