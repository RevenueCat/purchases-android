package com.revenuecat.purchases

internal data class ProductInfo(
    val productID: String,
    val offeringIdentifier: String?,
    val price: Double?,
    val currency: String?,
    val duration: String?,
    val introDuration: String?,
    val trialDuration: String?
)
