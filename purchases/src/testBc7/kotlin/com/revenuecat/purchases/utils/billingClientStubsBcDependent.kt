@file:Suppress("Filename")

package com.revenuecat.purchases.utils

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchaseHistoryRecord
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray

@SuppressWarnings("MagicNumber")
fun mockOneTimePurchaseOfferDetails(
    price: Double = 4.99,
    priceCurrencyCodeValue: String = "USD",
    offerToken: String = "mockOfferToken",
): ProductDetails.OneTimePurchaseOfferDetails = mockk<ProductDetails.OneTimePurchaseOfferDetails>().apply {
    every { formattedPrice } returns "${'$'}$price"
    every { priceAmountMicros } returns price.times(1_000_000).toLong()
    every { priceCurrencyCode } returns priceCurrencyCodeValue
    every { zzb() } returns offerToken
}

fun stubPurchaseHistoryRecord(
    productIds: List<String> = listOf("monthly_intro_pricing_one_week"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijkcopgbomfinlko.AO-J1OxJixLsieYN08n9hV4qBsvqvQo6wXesyAClWs-t7KnYLCm3-" +
        "q6z8adcZnenbzqMHuMIqZ9kQ4KebT_Bge6KfZUhBt-0N0U0s71AEwFpzT7hrtErzdg",
    signature: String = "signature${System.currentTimeMillis()}",
): PurchaseHistoryRecord = PurchaseHistoryRecord(
    """
            {
                "productIds": ${JSONArray(productIds)},
                "purchaseTime": $purchaseTime,
                "purchaseToken": "$purchaseToken"
            }
    """.trimIndent(),
    signature,
)
