package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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
        val run = presented(CheckpointFlowOutcome.Finished(customerInfo))

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

    @Test
    fun `the customer info a flow produced is the purchase, restore or sync one`() {
        val customerInfo = customerInfoWithActive("pro")
        val producing = listOf(
            CheckpointFlowOutcome.Purchased(customerInfo, mockk()),
            CheckpointFlowOutcome.Restored(customerInfo),
            CheckpointFlowOutcome.Finished(customerInfo),
        )
        val others = listOf(
            CheckpointFlowOutcome.Dismissed,
            CheckpointFlowOutcome.WebCheckoutOpened,
            CheckpointFlowOutcome.SyncFailed,
            CheckpointFlowOutcome.Error(PurchasesError(PurchasesErrorCode.StoreProblemError, "Simulated.")),
            object : CheckpointFlowOutcome() {},
        )

        producing.forEach { assertThat(it.customerInfo).isSameAs(customerInfo) }
        others.forEach { assertThat(it.customerInfo).isNull() }
    }

    @Test
    fun `a run delivers the customer info its flow produced without reading the cache`() = runTest {
        val produced = customerInfoWithActive("pro")
        val purchases = mockk<Purchases>()

        val run = presented(CheckpointFlowOutcome.Purchased(produced, mockk()))

        val delivered = run.customerInfoAfter(purchases, customerInfoBefore = mockk())

        assertThat(delivered).isSameAs(produced)
        verify(exactly = 0) { purchases.getCustomerInfo(any(), any()) }
    }

    @Test
    fun `a run whose flow produced nothing delivers the cached customer info, else the one read before`() = runTest {
        val cached = customerInfoWithActive()
        val before = customerInfoWithActive()
        val purchases = mockk<Purchases>()
        every { purchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(cached)
        }

        assertThat(presented(CheckpointFlowOutcome.Dismissed).customerInfoAfter(purchases, before)).isSameAs(cached)

        every { purchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>()
                .onError(PurchasesError(PurchasesErrorCode.CustomerInfoError, "No cache."))
        }

        assertThat(presented(CheckpointFlowOutcome.Dismissed).customerInfoAfter(purchases, before)).isSameAs(before)
        assertThat(CheckpointRun(flowOutcome = null, backedOut = false).customerInfoAfter(purchases, before))
            .isSameAs(before)
    }

    @Test
    fun `a customer info read delivers the customer info under the requested policy, or the error`() = runTest {
        val customerInfo = customerInfoWithActive()
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Simulated.")
        val purchases = mockk<Purchases> {
            every { getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfo)
            }
            every { getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>().onError(error)
            }
        }

        assertThat(purchases.customerInfo(CacheFetchPolicy.CACHED_OR_FETCHED).getOrNull()).isSameAs(customerInfo)
        assertThat(purchases.customerInfo(CacheFetchPolicy.FETCH_CURRENT).errorOrNull()).isEqualTo(error)
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
