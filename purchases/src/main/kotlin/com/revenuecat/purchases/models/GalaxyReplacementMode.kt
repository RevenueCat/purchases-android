package com.revenuecat.purchases.models

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ReplacementMode
import kotlinx.parcelize.Parcelize

/**
 * Enum of possible replacement modes to be passed to a Samsung Galaxy Store subscription change.
 * Not used for Google Play and Amazon purchases.
 *
 * See https://developer.samsung.com/iap/subscription-guide/manage-subscription-plan/proration-modes.html
 * for more details.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Parcelize
public enum class GalaxyReplacementMode : ReplacementMode {
    /**
     * The current subscription is instantly changed and the customer can start using the new subscription
     * immediately. The remaining payment of the original subscription is prorated to the cost of the new
     * subscription (based on the daily price). The payment (renewal) date and starting day of the subscription
     * period are changed based on this calculation.
     *
     * This mode can be used for both upgrades and downgrades.
     */
    INSTANT_PRORATED_DATE,

    /**
     * For upgraded subscriptions only. The current subscription is instantly changed and the customer can
     * start using the new subscription immediately. While the starting day of the subscription period and
     * payment (renewal) date remain the same, the prorated cost of the upgraded subscription for the remainder
     * of the subscription period (minus the remaining payment of the original subscription) is immediately
     * charged to the customer.
     */
    INSTANT_PRORATED_CHARGE,

    /**
     * For upgraded subscriptions only. The current subscription is instantly changed and the customer can
     * start using the new subscription immediately. The new subscription rate is not applied until the current
     * subscription period ends. The payment (renewal) date remains the same. There are no extra charges to use
     * the upgraded subscription during the current subscription period.
     *
     * This is the default behavior.
     */
    INSTANT_NO_PRORATION,

    /**
     * The current subscription continues and the features of the new subscription are not available until
     * the current subscription period ends. The new subscription price is charged and the features of the
     * new subscription are available when the subscription is renewed. When the customer changes their
     * subscription, they cannot change the subscription again during the remaining time of the current
     * subscription period.
     */
    DEFERRED,
    ;

    public companion object {
        /**
         * The default replacement mode for Galaxy Store subscription changes.
         */
        @InternalRevenueCatAPI
        public val default: GalaxyReplacementMode = INSTANT_NO_PRORATION
    }
}
