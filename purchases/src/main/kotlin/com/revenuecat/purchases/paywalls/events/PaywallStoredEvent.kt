package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.Serializable

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Serializable
internal data class PaywallStoredEvent(
    val event: PaywallEvent,
    val userID: String,
)
