package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.EventsRequest
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.events.CustomerCenterDisplayMode
import com.revenuecat.purchases.customercenter.events.CustomerCenterEventType
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import kotlinx.serialization.SerialName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import java.util.Date
import java.util.UUID

internal class ProductionVirtualCurrenciesIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey

    @Test
    fun `can fetch virtual currencies with a balance of 0`() {
        ensureBlockFinishes { latch ->
            backend.getVirtualCurrencies(
                appUserID = "integrationTestUserWithAllBalancesEqualTo0",
                appInBackground = false,
                onSuccess = { virtualCurrencies ->
                    validateAllZeroBalanceVirtualCurrenciesObject(virtualCurrencies = virtualCurrencies)
                    latch.countDown()
                },
                onError = { error ->
                    fail("Expected success but got error: $error")
                },
            )
        }
    }

    private fun validateAllZeroBalanceVirtualCurrenciesObject(virtualCurrencies: VirtualCurrencies) {
        assert(virtualCurrencies.all.count() == 3)

        val testCurrency = virtualCurrencies["TEST"]
        assert(testCurrency != null)
        assert(testCurrency?.balance == 0)
        assert(testCurrency?.code == "TEST")
        assert(testCurrency?.name == "Test Currency")
        assert(testCurrency?.serverDescription == "This is a test currency")

        val testCurrency2 = virtualCurrencies["TEST2"]
        assert(testCurrency2 != null)
        assert(testCurrency2?.balance == 0)
        assert(testCurrency2?.code == "TEST2")
        assert(testCurrency2?.name == "Test Currency 2")
        assert(testCurrency2?.serverDescription == "This is test currency 2")

        val testCurrency3 = virtualCurrencies["TEST3"]
        assert(testCurrency3 != null)
        assert(testCurrency3?.balance == 0)
        assert(testCurrency3?.code == "TEST3")
        assert(testCurrency3?.name == "Test Currency 3")
        assert(testCurrency3?.serverDescription == null)
    }
}
