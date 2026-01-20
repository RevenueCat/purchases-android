package com.revenuecat.purchases.paywalls

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType

@OptIn(InternalRevenueCatAPI::class)
internal class PaywallPresentedCache {
    @get:Synchronized
    @set:Synchronized
    private var lastPurchaseInitiatedEvent: PaywallEvent? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun hasCachedPurchaseInitiatedData(): Boolean {
        return lastPurchaseInitiatedEvent != null
    }

    @Synchronized
    fun getAndRemovePurchaseInitiatedEventIfNeeded(
        purchasedProductIDs: List<String>,
        purchaseTimestamp: Long?,
    ): PaywallEvent? {
        val shouldAttributePaywallToPurchase: Boolean = lastPurchaseInitiatedEvent?.let { event ->
            val wasPurchasePerformedAfterPurchaseInitiated = purchaseTimestamp?.let { timestamp ->
                event.creationData.date.time <= timestamp
            } ?: false
            event.type == PaywallEventType.PURCHASE_INITIATED &&
                event.data.productIdentifier in purchasedProductIDs &&
                wasPurchasePerformedAfterPurchaseInitiated
        } ?: false
        if (!shouldAttributePaywallToPurchase) {
            return null
        }
        val event = lastPurchaseInitiatedEvent
        lastPurchaseInitiatedEvent = null
        return event
    }

    @Synchronized
    fun receiveEvent(event: PaywallEvent) {
        when (event.type) {
            PaywallEventType.PURCHASE_INITIATED -> {
                verboseLog { "Caching paywall purchase initiated event." }
                lastPurchaseInitiatedEvent = event
            }
            PaywallEventType.CANCEL, PaywallEventType.PURCHASE_ERROR -> {
                verboseLog { "Clearing cached paywall purchase initiated event." }
                lastPurchaseInitiatedEvent = null
            }
            else -> {
            }
        }
    }
}
