package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Parameters for tracking a custom paywall impression event.
 *
 * @property paywallId An optional identifier for the custom paywall being shown.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public class CustomPaywallEventParams(
    public val paywallId: String? = null,
)
