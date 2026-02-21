package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes how a promotional offer was rejected by the user.
 */
@InternalRevenueCatAPI
@Serializable
public enum class PromoOfferRejectionSource(public val value: String) {
    /**
     * User tapped the X button (close/back button in navigation bar).
     */
    @SerialName("x-mark")
    X_MARK("x-mark"),

    /**
     * User tapped the "No thanks" or cancel button within the offer screen.
     */
    @SerialName("cancel")
    CANCEL("cancel"),
}
