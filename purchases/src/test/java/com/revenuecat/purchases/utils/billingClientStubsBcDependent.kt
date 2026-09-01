@file:Suppress("Filename")

package com.revenuecat.purchases.utils

import com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails
import io.mockk.every
import io.mockk.mockk

@SuppressWarnings("MagicNumber")
fun mockOneTimePurchaseOfferDetails(
    price: Double = 4.99,
    priceCurrencyCodeValue: String = "USD",
    offerTokenProvided: String = "mockOfferToken",
    offerIdProvided: String? = null,
    offerTagsProvided: List<String>? = emptyList(),
): OneTimePurchaseOfferDetails = mockk<OneTimePurchaseOfferDetails>().apply {
    every { formattedPrice } returns "${'$'}$price"
    every { priceAmountMicros } returns price.times(1_000_000).toLong()
    every { priceCurrencyCode } returns priceCurrencyCodeValue
    every { offerToken } returns offerTokenProvided
    every { offerId } returns offerIdProvided
    every { offerTags } returns offerTagsProvided
}
