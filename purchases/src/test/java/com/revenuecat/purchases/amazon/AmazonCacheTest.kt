package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.amazon.helpers.MockDeviceCache
import com.revenuecat.purchases.utils.JSONObjectAssert
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmazonCacheTest {
    private val apiKey = "api_key"
    private lateinit var cache: MockDeviceCache

    private lateinit var underTest: AmazonCache

    @Before
    fun setup() {
        cache = MockDeviceCache(mockk(), apiKey)
        underTest = AmazonCache(cache)
    }

    @Test
    fun `getting cached term skus when there is nothing cached`() {
        val receiptTermSkus = underTest.getReceiptSkus()

        assertThat(receiptTermSkus).isEmpty()
    }

    @Test
    fun `getting cached term skus when there is termskus cached`() {
        val expected = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly",
            "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        val cachedReceiptsToTermSkusJSON = getStoredJSONFromMap(expected)

        cache.stubCache[underTest.amazonPostedTokensKey] = cachedReceiptsToTermSkusJSON

        val receiptTermSkus = underTest.getReceiptSkus()

        assertThat(receiptTermSkus).isEqualTo(expected)
    }

    @Test
    fun `set receipt term skus on an empty cache`() {
        val expected = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly",
            "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        underTest.cacheSkusByToken(expected)

        val actualStoredJSON = JSONObject(cache.stubCache[underTest.amazonPostedTokensKey])
        val actualStoredMapAsJSON = actualStoredJSON["receiptsToSkus"] as JSONObject

        assertThat(actualStoredMapAsJSON).isNotNull
        JSONObjectAssert.assertThat(actualStoredMapAsJSON).isEqualToMap(expected)
    }

    @Test
    fun `set receipt term skus on a non empty cache`() {
        val alreadyCached = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly"
        )

        val cachedReceiptsToTermSkusJSON = getStoredJSONFromMap(alreadyCached)
        cache.stubCache[underTest.amazonPostedTokensKey] = cachedReceiptsToTermSkusJSON

        val newToCache = mapOf(
            "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        underTest.cacheSkusByToken(newToCache)

        val actualStoredJSON = JSONObject(cache.stubCache[underTest.amazonPostedTokensKey])
        val actualStoredMapAsJSON = actualStoredJSON["receiptsToSkus"] as JSONObject

        assertThat(actualStoredMapAsJSON).isNotNull

        val expected = alreadyCached + newToCache

        JSONObjectAssert.assertThat(actualStoredMapAsJSON).isEqualToMap(expected)
    }

    @Test
    fun `overriding a receipt term sku`() {
        val alreadyCached = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly"
        )

        val cachedReceiptsToTermSkusJSON = getStoredJSONFromMap(alreadyCached)
        cache.stubCache[underTest.amazonPostedTokensKey] = cachedReceiptsToTermSkusJSON

        val expected = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        underTest.cacheSkusByToken(expected)

        val actualStoredJSON = JSONObject(cache.stubCache[underTest.amazonPostedTokensKey])
        val actualStoredMapAsJSON = actualStoredJSON["receiptsToSkus"] as JSONObject

        assertThat(actualStoredMapAsJSON).isNotNull
        JSONObjectAssert.assertThat(actualStoredMapAsJSON).isEqualToMap(expected)
    }

    private fun getStoredJSONFromMap(expected: Map<String, String>) = """
                { "receiptsToSkus": 
                    {
                        ${expected.map { "\"${it.key}\": \"${it.value}\"" }.joinToString()}
                    }
                }
            """.trimIndent()
}
