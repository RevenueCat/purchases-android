package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for a specific Play Store Offer to use for a package in a paywall.
 * Similar to iOS's applePromoOfferProductCode.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class PlayStoreOfferConfig(
    /**
     * The Play Store offer identifier to use for this package.
     * This should match the offer ID configured in Google Play Console.
     */
    @get:JvmSynthetic
    @SerialName("offer_id")
    val offerId: String,
)
