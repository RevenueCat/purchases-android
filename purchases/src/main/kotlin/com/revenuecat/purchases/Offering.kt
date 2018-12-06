package com.revenuecat.purchases

import com.android.billingclient.api.SkuDetails

/**
 * Most well monetized subscription apps provide many different offerings to purchase an
 * entitlement. These are usually associated with different durations i.e. an annual plan and a
 * monthly plan.
 * See [this link](https://docs.revenuecat.com/docs/entitlements) for more info
 * @property activeProductIdentifier The currently active Play Store product for this offering
 * @property skuDetails Object containing an in-app product's or subscription's listing details
 */
data class Offering @JvmOverloads internal constructor(
    val activeProductIdentifier: String,
    var skuDetails: SkuDetails? = null
)
