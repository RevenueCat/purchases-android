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

/**
 * The primary source returns 5xx errors but its health check passes, so the SDK doesn't fail over to the backup
 * source.
 */
@RunWith(AndroidJUnit4::class)
class ProductionAPISourceServerErrorIntegrationTest : BaseAPISourcesIntegrationTest() {

    override fun createForceServerErrorStrategy(): ForceServerErrorStrategy =
        recorder.serverErrorStrategy(hosts = setOf(APIHosts.PRIMARY))

    @Test
    fun offeringsUseTheStaticFallbackWithoutFailingOver() = runTest {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()

        assertThat(offerings.all).isNotEmpty
        assertThat(recorder.hosts<Endpoint.GetOfferings>()).contains(APIHosts.FALLBACK)
        assertThat(recorder.requests.map { it.host }).doesNotContain(APIHosts.BACKUP)
    }
}
