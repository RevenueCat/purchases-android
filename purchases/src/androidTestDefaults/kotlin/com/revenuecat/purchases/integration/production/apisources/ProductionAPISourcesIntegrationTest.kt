package com.revenuecat.purchases.integration.production.apisources

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionAPISourcesIntegrationTest : BaseAPISourcesIntegrationTest() {

    override fun createForceServerErrorStrategy(): ForceServerErrorStrategy = recorder.recordingStrategy()

    @Test
    fun offeringsAreFetchedFromThePrimarySource() = runTest {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()

        assertThat(offerings.current).isNotNull
        assertThat(recorder.hosts<Endpoint.GetOfferings>()).containsOnly(APIHosts.PRIMARY)
    }

    @Test
    fun purchasesArePostedToThePrimarySource() = runTest {
        val customerInfo = purchase()

        assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(entitlementsToVerify)
        assertThat(recorder.hosts<Endpoint.PostReceipt>()).containsOnly(APIHosts.PRIMARY)
    }

    @Test
    fun logInUsesThePrimarySource() = runTest {
        Purchases.sharedInstance.awaitLogIn("$testUserId-logged-in")

        assertThat(recorder.hosts<Endpoint.LogIn>()).containsOnly(APIHosts.PRIMARY)
    }

    @Test
    fun remoteConfigIsFetchedFromThePrimarySource() {
        waitForRemoteConfigRequest(toHost = APIHosts.PRIMARY)

        assertThat(recorder.hosts<Endpoint.GetRemoteConfig>()).containsOnly(APIHosts.PRIMARY)
    }
}
