package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Factory interface for creating custom paywall handlers based on context parameters.
 *
 * This allows you to provide different [CustomPaywallHandler] implementations based on
 * the offering being displayed and other contextual information. The factory is called
 * once when a paywall is displayed, and the resulting handler is cached for the lifetime
 * of that paywall view.
 *
 * Set this factory via [PurchasesConfiguration.Builder.setCustomPaywallHandlerFactory].
 *
 * Example:
 * ```kotlin
 * val config = PurchasesConfiguration.Builder(context, apiKey)
 *     .setCustomPaywallHandlerFactory { params ->
 *         when (params.offering.identifier) {
 *             "premium" -> PremiumPaywallHandler()
 *             "basic" -> BasicPaywallHandler()
 *             else -> null  // No custom handler for other offerings
 *         }
 *     }
 *     .build()
 * ```
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
fun interface CustomPaywallHandlerFactory {
    /**
     * Creates a [CustomPaywallHandler] for the given parameters.
     *
     * This method is called when a paywall is about to be displayed.
     * Return null if you don't want to provide a custom handler for this paywall.
     *
     * @param params Context parameters including the offering being displayed
     * @return A [CustomPaywallHandler] instance, or null to skip providing handlers
     */
    @OptIn(InternalRevenueCatAPI::class)
    fun createCustomPaywallHandler(params: CustomPaywallHandlerParams): CustomPaywallHandler?
}
