package com.revenuecat.purchases

import com.android.billingclient.api.SkuDetails

data class Offering @JvmOverloads constructor(
    val activeProductIdentifier: String,
    var skuDetails: SkuDetails? = null
)
