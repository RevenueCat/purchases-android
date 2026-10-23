package com.revenuecat.purchases.integration.production.apisources

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Every source is down, so offerings fall back to the static fallback host. */
@RunWith(AndroidJUnit4::class)
class ProductionAPISourcesExhaustedIntegrationTest : BaseAPISourcesIntegrationTest() {

    override fun createForceServerErrorStrategy(): ForceServerErrorStrategy =
        recorder.outageStrategy(hosts = setOf(APIHosts.PRIMARY, APIHosts.BACKUP))

    @Test
    fun offeringsUseTheStaticFallback() = runTest {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()

        assertThat(offerings.all).isNotEmpty
        assertThat(recorder.hosts<Endpoint.GetOfferings>()).contains(APIHosts.BACKUP, APIHosts.FALLBACK)
    }
}
