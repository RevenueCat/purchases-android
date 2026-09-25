package com.revenuecat.purchases.models

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.SharedConstants

public class OneTimePurchaseOfferDetailsList(
    private val oneTimePurchaseOfferDetailsList: List<OneTimePurchaseOfferDetails>,
) : List<OneTimePurchaseOfferDetails> by oneTimePurchaseOfferDetailsList {

    private companion object {
        const val RC_IGNORE_OFFER_TAG = "rc-ignore-offer"
    }

    /**
     * The default [OneTimePurchaseOfferDetails]:
     *   - Filters out offers with "rc-ignore-offer" and "rc-customer-center" tag
     *   - Uses [OneTimePurchaseOfferDetails] with cheapest price (`amountMicros`)
     */
    @OptIn(InternalRevenueCatAPI::class)
    public val defaultOffer: OneTimePurchaseOfferDetails?
        get() {
            return this
                .filter { !(it.offerTags?.contains(RC_IGNORE_OFFER_TAG) ?: false) }
                .filter { !(it.offerTags?.contains(SharedConstants.RC_CUSTOMER_CENTER_TAG) ?: false) }
                .minByOrNull { it.price.amountMicros }
        }

    /**
     * The base plan [OneTimePurchaseOfferDetails] (the offer detail with null [discountDisplayInfo]).
     */
    public val basePlan: OneTimePurchaseOfferDetails?
        get() = this.firstOrNull { it.discountDisplayInfo == null }

    /**
     * Finds all [OneTimePurchaseOfferDetails] with a specific tag.
     */
    public fun withTag(tag: String): List<OneTimePurchaseOfferDetails> {
        return this.filter { it.offerTags?.contains(tag) ?: false }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        (other as? OneTimePurchaseOfferDetailsList)?.let {
            return listOf(this.oneTimePurchaseOfferDetailsList) == listOf(other.oneTimePurchaseOfferDetailsList)
        } ?: run {
            return false
        }
    }

    override fun hashCode(): Int {
        return listOf(this.oneTimePurchaseOfferDetailsList).hashCode()
    }
}
