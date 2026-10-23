package com.revenuecat.purchases.integration.production.apisources

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import io.mockk.every
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Rule

/**
 * Runs a normal [Purchases] instance against production with API sources enabled.
 * [createForceServerErrorStrategy] decides which hosts are failing, and [recorder] tracks where requests went.
 */
abstract class BaseAPISourcesIntegrationTest : BasePurchasesIntegrationTest() {

    private companion object {
        const val POLL_INTERVAL_MS = 100L
    }

    override val environmentConfig get() = Constants.production

    @OptIn(InternalRevenueCatAPI::class)
    override val dangerousSettings: DangerousSettings = DangerousSettings(usesRemoteConfigAPISources = true)

    // A proxy pins every request to its host, bypassing API sources.
    override val proxyUrl: String? = null

    internal val recorder = APISourceRecorder()

    internal abstract fun createForceServerErrorStrategy(): ForceServerErrorStrategy

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        forceServerErrorsStrategy = createForceServerErrorStrategy()
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    protected suspend fun purchase(): CustomerInfo {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()
        val storeTransaction = StoreTransactionFactory.createStoreTransaction()
        mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
        every {
            mockBillingAbstract.makePurchaseAsync(any(), any(), any(), any(), any(), any())
        } answers {
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(storeTransaction))
        }

        return Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, storeProduct).build())
            .customerInfo
    }

    /** Remote config is fetched in the background when the app is foregrounded, so it has to be waited for. */
    protected fun waitForRemoteConfigRequest(toHost: String) {
        val deadline = System.currentTimeMillis() + testTimeout.inWholeMilliseconds
        while (toHost !in recorder.hosts<Endpoint.GetRemoteConfig>()) {
            if (System.currentTimeMillis() > deadline) {
                fail<Unit>("Remote config was never requested from $toHost. Requests: ${recorder.requests}")
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }
}
