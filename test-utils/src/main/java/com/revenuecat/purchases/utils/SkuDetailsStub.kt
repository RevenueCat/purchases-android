package com.revenuecat.purchases.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails

@SuppressWarnings("LongParameterList", "MagicNumber")
fun stubSkuDetails(
    productId: String = "monthly_intro_pricing_one_week",
    @BillingClient.SkuType type: String = BillingClient.SkuType.SUBS,
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
