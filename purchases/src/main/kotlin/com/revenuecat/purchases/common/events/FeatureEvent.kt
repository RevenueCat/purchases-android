package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.events.CustomerCenterEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
sealed class FeatureEvent {
    data class Paywall(val event: PaywallEvent) : FeatureEvent()
    data class CustomerCenter(val event: CustomerCenterEvent) : FeatureEvent()
}
