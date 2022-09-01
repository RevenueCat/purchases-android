package com.revenuecat.purchases.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.SkuDetails
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.json.JSONArray

@SuppressWarnings("LongParameterList", "MagicNumber")
fun stubSkuDetails(
    productId: String = "monthly_intro_pricing_one_week",
    @BillingClient.ProductType type: String = BillingClient.ProductType.SUBS,
    price: Double = 4.99,
    subscriptionPeriod: String = "P1M",
    freeTrialPeriod: String? = null,
    introductoryPricePeriod: String? = null
): SkuDetails = SkuDetails("""
            {
              "skuDetailsToken":"AEuhp4KxWQR-b-OAOXVicqHM4QqnqK9vkPnOXw0vSB9zWPBlTsW8TmtjSEJ_rJ6f0_-i",
              "productId":"$productId",
              "type":"$type",
              "price":"${'$'}$price",
              "price_amount_micros":${price.times(1_000_000)},
              "price_currency_code":"USD",
              "subscriptionPeriod":"$subscriptionPeriod",
              "freeTrialPeriod":"$freeTrialPeriod",
              ${ introductoryPricePeriod?.let { """
                  "introductoryPricePeriod":"$it",
                  "introductoryPriceAmountMicros":990000,
                  "introductoryPrice":"${'$'}0.99",
                  "introductoryPriceCycles":2,
              """.trimIndent() } ?: "" }
              "title":"Monthly Product Intro Pricing One Week (PurchasesSample)",
              "description":"Monthly Product Intro Pricing One Week"
            }    
        """.trimIndent())

fun stubGooglePurchase(
    productIds: List<String> = listOf("com.revenuecat.lifetime"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijipehfnbbnldmai.AO-J1OxqriTepvB7suzlIhxqPIveA0IHtX9amMedK0KK9CsO0S3Zk5H6gdwvV" +
        "7HzZIJeTzqkY4okyVk8XKTmK1WZKAKSNTKop4dgwSmFnLWsCxYbahUmADg",
    signature: String = "signature${System.currentTimeMillis()}",
    purchaseState: Int = Purchase.PurchaseState.PURCHASED,
    acknowledged: Boolean = true,
    orderId: String = "GPA.3372-4150-8203-17209"
): Purchase = Purchase(
    """
    {
        "orderId": "$orderId",
        "packageName":"com.revenuecat.purchases_sample",
        "productIds":${JSONArray(productIds)},
        "purchaseTime":$purchaseTime,
        "purchaseState":${if (purchaseState == 2) 4 else 1},
        "purchaseToken":"$purchaseToken",
        "acknowledged":$acknowledged
    }
        """.trimIndent(), signature
)

fun stubPurchaseHistoryRecord(
    productIds: List<String> = listOf("monthly_intro_pricing_one_week"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijkcopgbomfinlko.AO-J1OxJixLsieYN08n9hV4qBsvqvQo6wXesyAClWs-t7KnYLCm3-" +
        "q6z8adcZnenbzqMHuMIqZ9kQ4KebT_Bge6KfZUhBt-0N0U0s71AEwFpzT7hrtErzdg",
    signature: String = "signature${System.currentTimeMillis()}"
): PurchaseHistoryRecord = PurchaseHistoryRecord("""
            {
                "productIds": ${JSONArray(productIds)},
                "purchaseTime": $purchaseTime,
                "purchaseToken": "$purchaseToken"
            }
        """.trimIndent(), signature)

fun BillingClient.mockQueryPurchaseHistory(
    result: BillingResult,
    history: List<PurchaseHistoryRecord>
): QueryPurchaseHistoryParams.Builder {
    mockkStatic(QueryPurchaseHistoryParams::class)

    val mockBuilder = mockk<QueryPurchaseHistoryParams.Builder>(relaxed = true)
    every {
        QueryPurchaseHistoryParams.newBuilder()
    } returns mockBuilder

    every {
        mockBuilder.setProductType(any())
    } returns mockBuilder

    val params = mockk<QueryPurchaseHistoryParams>(relaxed = true)
    every {
        mockBuilder.build()
    } returns params

    val billingClientPurchaseHistoryListenerSlot = slot<PurchaseHistoryResponseListener>()

    every {
        queryPurchaseHistoryAsync(
            params,
            capture(billingClientPurchaseHistoryListenerSlot)
        )
    } answers {
        billingClientPurchaseHistoryListenerSlot.captured.onPurchaseHistoryResponse(
            result,
            history
        )
    }
    return mockBuilder
}
