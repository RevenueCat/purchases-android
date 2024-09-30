package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

internal data class SubscriptionInformation(
    val title: String,
    val durationTitle: String,
    val price: String,
    val expirationDateString: String?,
    val productIdentifier: String,
    val willRenew: Boolean,
    val active: Boolean,
)
