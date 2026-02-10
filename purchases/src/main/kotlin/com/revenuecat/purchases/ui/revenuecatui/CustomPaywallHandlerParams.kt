package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import dev.drewhamilton.poko.Poko

/**
 * Parameters passed to [CustomPaywallHandlerFactory] when creating a custom paywall handler.
 *
 * This class encapsulates all context needed to create an appropriate handler for a paywall.
 * Currently contains the offering being displayed, but may be extended with additional
 * parameters in the future.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
class CustomPaywallHandlerParams @InternalRevenueCatAPI constructor(
    /**
     * The offering for which the paywall is being displayed.
     */
    val offering: Offering,
)
