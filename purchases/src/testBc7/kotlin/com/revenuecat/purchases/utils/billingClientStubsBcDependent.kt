@file:Suppress("Filename")

package com.revenuecat.purchases.utils

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk

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
