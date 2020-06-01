package com.revenuecat.purchases

internal data class ProductInfo(
    val productID: String,
    val offeringIdentifier: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val duration: String? = null,
    val introDuration: String? = null,
    val trialDuration: String? = null
)
