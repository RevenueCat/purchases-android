package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for a promo offer to use for a package in a paywall.
 * On Android, this maps to a Play Store offer ID.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class PromoOfferConfig(
    /**
     * The offer identifier to use for this package.
     * On Android, this should match the offer ID configured in Google Play Console.
     */
    @get:JvmSynthetic
    @SerialName("offer_id")
    val offerId: String,
)
