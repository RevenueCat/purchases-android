package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct
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
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FlowResultMappingTest {

    @Test
    fun `a run that presented nothing has no result`() {
        val run = CheckpointRun(flowOutcome = null, backedOut = false)

        assertThat(run.toResult(customerInfoWithActive(), customerInfoWithActive())).isNull()
    }

    @Test
    fun `a presented run reports what the delivered customer info holds over the one read before`() {
        val monthly = product("monthly")
        val before = customerInfoWithActive("plus").purchased("annual")
        val delivered = customerInfoWithActive("plus", "pro").purchased("monthly", "annual")
        val run = presented(CheckpointFlowOutcome.Dismissed, presentedProducts = listOf(monthly, product("annual")))

        val result = run.toResult(before, delivered)

        assertThat(result.obtainedIdentifiers()).containsExactly("pro")
        assertThat(result?.obtainedProducts).containsExactly(ObtainedProduct(monthly))
    }

    @Test
    fun `an obtained entitlement carries the full entitlement info`() {
        val delivered = customerInfoWithActive("pro")
        val run = presented(CheckpointFlowOutcome.Purchased(delivered, mockk()))

        val obtained = run.toResult(customerInfoWithActive(), delivered)?.obtainedEntitlements?.single()
        assertThat(obtained?.entitlementInfo).isSameAs(delivered.entitlements.active.getValue("pro"))
    }

    @Test
    fun `a run whose delivered customer info holds nothing new yields an empty result`() {
        val before = customerInfoWithActive("pro").purchased("monthly")
        val delivered = customerInfoWithActive("pro").purchased("monthly")
        listOf(
            CheckpointFlowOutcome.Dismissed,
            CheckpointFlowOutcome.WebCheckoutOpened,
            CheckpointFlowOutcome.SyncFailed,
            object : CheckpointFlowOutcome() {},
        ).forEach { outcome ->
            val run = presented(outcome, presentedProducts = listOf(product("monthly")))

            assertThat(run.toResult(before, delivered)).isEqualTo(FlowResult(emptySet(), emptySet()))
        }
    }

    @Test
    fun `an in-flow error yields no result`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Simulated.")
        val run = presented(CheckpointFlowOutcome.Error(error))

        assertThat(run.toResult(customerInfoWithActive(), customerInfoWithActive("pro"))).isNull()
    }

    @Test
    fun `without a snapshot every active entitlement counts as obtained`() {
        val obtained = customerInfoWithActive("pro", "extra").obtainedEntitlements(activeEntitlementsBefore = null)

        assertThat(obtained.map { it.entitlementInfo.identifier }).containsExactlyInAnyOrder("extra", "pro")
    }

    @Test
    fun `a purchase obtains the presented products whose purchase date is new or changed`() {
        val monthly = product("monthly")
        val annual = product("annual")
        val lifetime = product("lifetime")
        val before = customerInfoWithActive().purchased("annual", date = Date(1_000)).purchased("lifetime")
        val after = customerInfoWithActive()
            .purchased("monthly")
            .purchased("annual", date = Date(2_000))
            .purchased("lifetime")

        val obtained = after.obtainedProducts(before, listOf(monthly, annual, lifetime))

        assertThat(obtained).containsExactlyInAnyOrder(ObtainedProduct(monthly), ObtainedProduct(annual))
    }

    @Test
    fun `products outside the presented ones are never reported`() {
        val before = customerInfoWithActive()
        val after = customerInfoWithActive().purchased("other")

        assertThat(after.obtainedProducts(before, listOf(product("monthly")))).isEmpty()
        assertThat(after.obtainedProducts(before, emptyList())).isEmpty()
        verify(exactly = 0) { after.getPurchaseDateForProductId("other") }
    }

    @Test
    fun `a purchase date keyed without the base plan is found through the plain product id`() {
        val monthly = subscriptionProduct(id = "monthly:p1m", productId = "monthly")
        val before = customerInfoWithActive()
        val after = customerInfoWithActive().purchased("monthly")

        assertThat(after.obtainedProducts(before, listOf(monthly))).containsExactly(ObtainedProduct(monthly))
    }

    @Test
    fun `the same product presented by several packages is reported once`() {
        val first = product("monthly")
        val second = product("monthly")
        val after = customerInfoWithActive().purchased("monthly")

        val obtained = after.obtainedProducts(customerInfoWithActive(), listOf(first, second))

        assertThat(obtained).hasSize(1)
        assertThat(obtained.single().product).isSameAs(first)
    }

    @Test
    fun `obtained products are the same when they share the store product id`() {
        val monthly = ObtainedProduct(product("monthly"))

        assertThat(monthly).isEqualTo(ObtainedProduct(product("monthly")))
        assertThat(monthly.hashCode()).isEqualTo(ObtainedProduct(product("monthly")).hashCode())
        assertThat(monthly).isNotEqualTo(ObtainedProduct(product("annual")))
    }

    @Test
    fun `obtainedProduct and didObtainProduct match on the store product id`() {
        val monthly = subscriptionProduct(id = "monthly:p1m", productId = "monthly")
        val result = FlowResult(obtainedEntitlements = emptySet(), obtainedProducts = setOf(ObtainedProduct(monthly)))

        assertThat(result.obtainedProduct("monthly")?.product).isSameAs(monthly)
        assertThat(result.didObtainProduct("monthly")).isTrue
        assertThat(result.obtainedProduct("monthly:p1m")).isNull()
        assertThat(result.didObtainProduct("annual")).isFalse
    }

    @Test
    fun `obtainedEntitlement and didObtainEntitlement match on the entitlement id`() {
        val pro = customerInfoWithActive("pro").entitlements.active.getValue("pro")
        val result = FlowResult(obtainedEntitlements = setOf(ObtainedEntitlement(pro)), obtainedProducts = emptySet())

        assertThat(result.obtainedEntitlement("pro")?.entitlementInfo).isSameAs(pro)
        assertThat(result.didObtainEntitlement("pro")).isTrue
        assertThat(result.obtainedEntitlement("plus")).isNull()
        assertThat(result.didObtainEntitlement("plus")).isFalse
    }

    @Test
    fun `a checkpoint result delegates lookups to its flow and answers nothing without one`() {
        val monthly = product("monthly")
        val pro = customerInfoWithActive("pro").entitlements.active.getValue("pro")
        val flow = FlowResult(setOf(ObtainedEntitlement(pro)), setOf(ObtainedProduct(monthly)))
        val withFlow = CheckpointResult(flow, customerInfoWithActive())
        val withoutFlow = CheckpointResult(flow = null, customer = customerInfoWithActive())

        assertThat(withFlow.obtainedEntitlement("pro")?.entitlementInfo).isSameAs(pro)
        assertThat(withFlow.didObtainEntitlement("pro")).isTrue
        assertThat(withFlow.obtainedProduct("monthly")?.product).isSameAs(monthly)
        assertThat(withFlow.didObtainProduct("monthly")).isTrue
        assertThat(withoutFlow.obtainedEntitlement("pro")).isNull()
        assertThat(withoutFlow.didObtainEntitlement("pro")).isFalse
        assertThat(withoutFlow.obtainedProduct("monthly")).isNull()
        assertThat(withoutFlow.didObtainProduct("monthly")).isFalse
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

    private fun presented(outcome: CheckpointFlowOutcome, presentedProducts: List<StoreProduct> = emptyList()) =
        CheckpointRun(outcome, backedOut = false, presentedProducts = presentedProducts)

    private fun FlowResult?.obtainedIdentifiers(): List<String>? =
        this?.obtainedEntitlements?.map { it.entitlementInfo.identifier }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo {
        val active = identifiers.associateWith { id -> mockk<EntitlementInfo> { every { identifier } returns id } }
        return mockk {
            every { entitlements.active } returns active
            every { getPurchaseDateForProductId(any()) } returns null
        }
    }

    private fun CustomerInfo.purchased(vararg identifiers: String, date: Date = Date(1_000)): CustomerInfo = apply {
        identifiers.forEach { identifier -> every { getPurchaseDateForProductId(identifier) } returns date }
    }

    private fun product(identifier: String): StoreProduct =
        TestStoreProduct(identifier, "name", "title", "description", Price("$1.99", 1_990_000, "USD"))

    private fun subscriptionProduct(id: String, productId: String): StoreProduct = mockk {
        every { this@mockk.id } returns id
        every { purchasingData.productId } returns productId
    }
}
