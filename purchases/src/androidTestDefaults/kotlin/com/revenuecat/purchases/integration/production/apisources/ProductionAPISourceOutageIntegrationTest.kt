package com.revenuecat.purchases.integration.production.apisources

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The primary source is down, so requests fail over to the backup source instead of the static fallback host.
 */
@RunWith(AndroidJUnit4::class)
class ProductionAPISourceOutageIntegrationTest : BaseAPISourcesIntegrationTest() {

    override fun createForceServerErrorStrategy(): ForceServerErrorStrategy =
        recorder.outageStrategy(hosts = setOf(APIHosts.PRIMARY))

    @Test
    fun offeringsAreFetchedFromTheBackupSource() = runTest {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()

        assertThat(offerings.current).isNotNull
        assertThat(recorder.hosts<Endpoint.GetOfferings>())
            .contains(APIHosts.BACKUP)
            .doesNotContain(APIHosts.FALLBACK)
    }

    @Test
    fun customerInfoIsFetchedFromTheBackupSource() = runTest {
        val customerInfo = Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)

        assertThat(customerInfo.loadedFromCache).isFalse
        assertThat(recorder.hosts<Endpoint.GetCustomerInfo>()).contains(APIHosts.BACKUP)
    }

    @Test
    fun purchasesArePostedToTheBackupSource() = runTest {
        val customerInfo = purchase()

        assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(entitlementsToVerify)
        assertThat(recorder.hosts<Endpoint.PostReceipt>()).contains(APIHosts.BACKUP)
    }

    @Test
    fun logInUsesTheBackupSource() = runTest {
        Purchases.sharedInstance.awaitLogIn("$testUserId-logged-in")

        assertThat(recorder.hosts<Endpoint.LogIn>()).contains(APIHosts.BACKUP)
    }

    @Test
    fun remoteConfigIsFetchedFromTheBackupSource() {
        waitForRemoteConfigRequest(toHost = APIHosts.BACKUP)
    }
}
