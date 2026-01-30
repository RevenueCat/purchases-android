package com.revenuecat.purchases

import com.revenuecat.purchases.common.events.FeatureEvent

/**
 * Listener interface for receiving tracked feature events.
 * This is an internal debug API for monitoring events tracked by RevenueCatUI.
 */
@InternalRevenueCatAPI
fun interface TrackedEventListener {
    /**
     * Called when a feature event is tracked.
     * @param event The tracked feature event.
     */
    fun onEventTracked(event: FeatureEvent)
}
