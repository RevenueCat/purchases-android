package com.revenuecat.purchases.virtualcurrencies

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualCurrencyTest {

    private val virtualCurrency = VirtualCurrency(
        name = "Test VC",
        balance = 100,
        code = "TEST",
        serverDescription = "hello world"
    )


    // region Equality Tests
    @Test
    fun `equals is true for two VirtualCurrency instances with the same values`() {
        val virtualCurrencyCopy = VirtualCurrency(
            name = "Test VC",
            balance = 100,
            code = "TEST",
            serverDescription = "hello world"
        )

        assertTrue(virtualCurrency == virtualCurrencyCopy)
    }

    @Test
    fun `equals is false for two VirtualCurrency instances with the same metadata but different balances`() {
        val virtualCurrencyCopy = VirtualCurrency(
            name = "Test VC",
            balance = 777,
            code = "TEST",
            serverDescription = "hello world"
        )

        assertFalse(virtualCurrency == virtualCurrencyCopy)
    }

    @Test
    fun `equals is false for two VirtualCurrency instances with different values`() {
        val virtualCurrency2 = VirtualCurrency(
            name = "Test VC 2",
            balance = 200,
            code = "TEST2",
            serverDescription = "lorem ipsum"
        )

        assertFalse(virtualCurrency == virtualCurrency2)
    }
    //endregion
}