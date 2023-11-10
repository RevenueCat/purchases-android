package com.revenuecat.purchases.paywalls

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class PaywallPresentedCache {
    @get:Synchronized
    @set:Synchronized
    private var lastPaywallImpressionEvent: PaywallEvent? = null

    @Synchronized
    fun getAndRemovePresentedEvent(): PaywallEvent? {
        val event = lastPaywallImpressionEvent
        lastPaywallImpressionEvent = null
        return event
    }

    @Synchronized
    fun cachePresentedPaywall(paywallEvent: PaywallEvent) {
        lastPaywallImpressionEvent = paywallEvent
    }

    @Synchronized
    fun receiveEvent(event: PaywallEvent) {
        when (event.type) {
            PaywallEventType.IMPRESSION -> {
                verboseLog("Caching paywall impression event.")
                lastPaywallImpressionEvent = event
            }
            PaywallEventType.CLOSE -> {
                verboseLog("Clearing cached paywall impression event.")
                lastPaywallImpressionEvent = null
            }
            else -> {
            }
        }
    }
}
