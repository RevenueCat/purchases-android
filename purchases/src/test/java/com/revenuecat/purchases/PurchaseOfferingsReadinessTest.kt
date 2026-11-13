package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchaseOfferingsReadinessTest : BasePurchasesTest() {
    @Test
    fun `purchase facade resolves cached package and launches billing while paywall config is pending`() {
        val offerings = stubOfferings("monthly-product").second
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

        purchases.getOfferingsForPurchase(object : ReceiveOfferingsCallback {
            override fun onError(error: PurchasesError) = throw AssertionError(error)
            override fun onReceived(offerings: Offerings) {
                val selectedPackage = offerings.current!!.availablePackages.first()
                purchases.purchase(PurchaseParams.Builder(mockActivity, selectedPackage).build(), mockk())
            }
        })

        verify(exactly = 1) { mockBillingAbstract.makePurchaseAsync(any(), appUserId, any(), any(), any(), any()) }
        assertThat(ordinaryCallbacks).isZero()
        pendingReadiness.forEach { it() }
        assertThat(ordinaryCallbacks).isEqualTo(1)
        verify(exactly = 1) { mockBillingAbstract.makePurchaseAsync(any(), appUserId, any(), any(), any(), any()) }
    }
}
