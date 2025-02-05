package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.events.CustomerCenterEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent

/**
 * Feature related events in RevenueCat.
 *
 * **RevenueCatUI** features should define their events here.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
sealed class FeatureEvent {

    /**
     * Represents an event related to paywalls.
     *
     * @property event The associated `PaywallEvent`.
     */
    data class Paywall(val event: PaywallEvent) : FeatureEvent()

    /**
     * Represents an event related to the Customer Center.
     *
     * @property event The associated `CustomerCenterEvent`.
     */
    data class CustomerCenter(val event: CustomerCenterEvent) : FeatureEvent()
}
