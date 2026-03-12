package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import dev.drewhamilton.poko.Poko

/**
 * Parameters for tracking a custom paywall impression event.
 *
 * @property paywallId An optional identifier for the custom paywall being shown.
 */
@Poko
@ExperimentalPreviewRevenueCatPurchasesAPI
public class CustomPaywallImpressionParams @JvmOverloads constructor(
    public val paywallId: String? = null,
)
