package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.events.CustomerCenterEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent

interface FeatureEvent {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    val paywallEvent: PaywallEvent? get() = null

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    val customerCenterEvent: CustomerCenterEvent? get() = null
}
