package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.interfaces.GetPackagesForPurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(InternalRevenueCatAPI::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchaseOfferingsReadinessTest : BasePurchasesTest() {
    @Test
    fun `purchase facade resolves cached package and launches billing while paywall config is pending`() {
        val main = googlePackage("main-offering", "monthly", "main-product")
        val addOn = googlePackage("other-offering", "monthly", "add-on-product")
        val otherOffering = Offering("other-offering", "Other", emptyMap(), listOf(addOn))
        val mainOffering = Offering("main-offering", "Main", emptyMap(), listOf(main))
        val offerings = Offerings(otherOffering, mapOf("main-offering" to mainOffering, "other-offering" to otherOffering))
        val cache = mockk<OfferingsCache>()
        every { cache.cachedOfferings } returns offerings
        every { cache.isOfferingsCacheStale(false) } returns false
        val pendingReadiness = mutableListOf<() -> Unit>()
        every { mockWorkflowManager.onPaywallConfigReady(capture(pendingReadiness)) } just Runs
        val manager = OfferingsManager(
            offeringsCache = cache,
            backend = mockBackend,
            offeringsFactory = mockk(),
            offeringImagePreDownloader = mockk(),
            diagnosticsTrackerIfEnabled = null,
            offeringFontPreDownloader = mockk(),
            offeringWebViewPrewarmer = mockk(),
            dispatcher = SyncDispatcher(),
            workflowManager = mockWorkflowManager,
        )
        buildPurchases(anonymous = false, offeringsManager = manager)
        var ordinaryCallbacks = 0
        purchases.getOfferings(object : ReceiveOfferingsCallback {
            override fun onError(error: PurchasesError) = throw AssertionError(error)
            override fun onReceived(offerings: Offerings) { ordinaryCallbacks++ }
        })

        purchases.getPackagesForPurchase(
            PackagePurchaseReference("main-offering", "MONTHLY"),
            listOf(PackagePurchaseReference("other-offering", "monthly")),
            object : GetPackagesForPurchaseCallback {
                override fun onError(error: PurchasesError) = throw AssertionError(error)
                override fun onReceived(packageToPurchase: Package, addOnPackages: List<Package>) {
                    assertThat(packageToPurchase).isSameAs(main)
                    assertThat(addOnPackages).containsExactly(addOn)
                    purchases.purchase(
                        PurchaseParams.Builder(mockActivity, packageToPurchase).addOnPackages(addOnPackages).build(),
                        mockk(),
                    )
                }
            },
        )

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(), appUserId,
                match {
                    val data = it as? GooglePurchasingData.Subscription
                    data?.productId == "main-product" && data.addOnProducts?.map { addOn -> addOn.productId } ==
                        listOf("add-on-product")
                },
                any(), any(), any(),
            )
        }
        assertThat(ordinaryCallbacks).isZero()
        pendingReadiness.forEach { it() }
        assertThat(ordinaryCallbacks).isEqualTo(1)
        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(), appUserId,
                match {
                    val data = it as? GooglePurchasingData.Subscription
                    data?.productId == "main-product" && data.addOnProducts?.map { addOn -> addOn.productId } ==
                        listOf("add-on-product")
                },
                any(), any(), any(),
            )
        }
    }
    private fun googlePackage(offeringId: String, packageId: String, productId: String): Package {
        val data = GooglePurchasingData.Subscription(
            productId, "monthly-option", mockk(), "offer-token",
            billingPeriod = Period(1, Period.Unit.MONTH, "P1M"),
        )
        val product = mockk<StoreProduct>()
        every { product.purchasingData } returns data
        return Package(packageId, PackageType.CUSTOM, product, PresentedOfferingContext(offeringId))
    }
}
