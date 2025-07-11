package com.revenuecat.purchases.backend_integration_tests


import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test

internal class ProductionVirtualCurrenciesIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey

    @Test
    fun `can fetch virtual currencies with a balance of 0`() {
        ensureBlockFinishes { latch ->
            backend.getVirtualCurrencies(
                appUserID = "integrationTestUserWithAllBalancesEqualTo0",
                appInBackground = false,
                onSuccess = { virtualCurrencies ->
                    validateVirtualCurrenciesObject(
                        virtualCurrencies = virtualCurrencies,
                        testVCBalance = 0,
                        testVC2Balance = 0
                    )
                    latch.countDown()
                },
                onError = { error ->
                    fail("Expected success but got error: $error")
                },
            )
        }
    }

    @Test
    fun `can fetch virtual currencies with a balance of greater than 0`() {
        ensureBlockFinishes { latch ->
            backend.getVirtualCurrencies(
                appUserID = "integrationTestUserWithAllBalancesNonZero",
                appInBackground = false,
                onSuccess = { virtualCurrencies ->
                    validateVirtualCurrenciesObject(
                        virtualCurrencies = virtualCurrencies,
                        testVCBalance = 100,
                        testVC2Balance = 777,
                    )
                    latch.countDown()
                },
                onError = { error ->
                    fail("Expected success but got error: $error")
                },
            )
        }
    }

    private fun validateVirtualCurrenciesObject(
        virtualCurrencies: VirtualCurrencies,
        testVCBalance: Int,
        testVC2Balance: Int,
        testVC3Balance: Int = 0,
    ) {
        assert(virtualCurrencies.all.count() == 3)

        val expectedTestVirtualCurrency = VirtualCurrency(
            code = "TEST",
            name = "Test Currency",
            balance = testVCBalance,
            serverDescription = "This is a test currency",
        )
        assertThat(virtualCurrencies["TEST"]).isEqualTo(expectedTestVirtualCurrency)

        val expectedTestVirtualCurrency2 = VirtualCurrency(
            code = "TEST2",
            name = "Test Currency 2",
            balance = testVC2Balance,
            serverDescription = "This is test currency 2",
        )
        assertThat(virtualCurrencies["TEST2"]).isEqualTo(expectedTestVirtualCurrency2)

        val expectedTestVirtualCurrency3 = VirtualCurrency(
            code = "TEST3",
            name = "Test Currency 3",
            balance = testVC3Balance,
            serverDescription = null,
        )
        assertThat(virtualCurrencies["TEST3"]).isEqualTo(expectedTestVirtualCurrency3)
    }
}
