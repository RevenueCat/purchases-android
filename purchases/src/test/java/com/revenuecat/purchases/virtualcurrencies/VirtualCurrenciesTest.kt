package com.revenuecat.purchases.virtualcurrencies

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class VirtualCurrenciesTest {

    private val virtualCurrency = VirtualCurrency(
        name = "Test VC",
        balance = 100,
        code = "TEST",
        serverDescription = "hello world"
    )

    // region Subscript Tests
    @Test
    fun `subscript returns correct virtual currency`() {
        val code = "COIN"
        val currency = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = code,
            serverDescription = "Coin currency"
        )
        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(code to currency),
        )

        val result = virtualCurrencies[code]
        assertEquals(currency, result)
    }

    @Test
    fun `subscript returns null for non-existent currency`() {
        val code = "COIN"
        val currency = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = code,
            serverDescription = "Coin currency"
        )
        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(code to currency),
        )

        assertNull(virtualCurrencies["NON_EXISTENT"])
    }
    // endregion

    // region Equality Tests
    @Test
    fun `equals is true for VirtualCurrencies objects with identical VCs`() {

        val vcClone = VirtualCurrency(
            name = "Test VC",
            balance = 100,
            code = "TEST",
            serverDescription = "hello world"
        )

        val virtualCurrencies1 = VirtualCurrencies(
            all = mapOf("TEST" to virtualCurrency)
        )
        val virtualCurrencies2 = VirtualCurrencies(
            all = mapOf("TEST" to virtualCurrency)
        )
        val virtualCurrencies3 = VirtualCurrencies(
            all = mapOf("TEST" to vcClone)
        )

        assertTrue(virtualCurrencies1 == virtualCurrencies2)
        assertTrue(virtualCurrencies1 == virtualCurrencies3)
    }

    @Test
    fun `equals is false for VirtualCurrencies objects with different VCs`() {

        val differentVirtualCurrency = VirtualCurrency(
            name = "Test VC 2",
            balance = 200,
            code = "TEST2",
            serverDescription = "lorem ipsum"
        )

        val virtualCurrencies1 = VirtualCurrencies(
            all = mapOf("TEST" to virtualCurrency)
        )
        val virtualCurrencies2 = VirtualCurrencies(
            all = mapOf("TEST2" to differentVirtualCurrency)
        )

        assertFalse(virtualCurrencies1 == virtualCurrencies2)
    }

    // endregion
}