package com.revenuecat.purchases.models

import com.revenuecat.purchases.PresentedOfferingContext

/**
 * Represents an offer/discount details for a one-time (INAPP) product.
 */
public interface OneTimePurchaseOfferDetails {
    /**
     * Product ID of the one-time product.
     */
    public val productId: String

    /**
     * Price details for the one-time purchase offer.
     */
    public val price: Price

    /**
     * Offer ID set in the Play Console if this offer has an ID, null otherwise.
     */
    public val offerId: String?

    /**
     * The offer token required to purchase this one-time offer via Google Play Billing.
     */
    public val offerToken: String?

    /**
     * Tags defined on the one-time purchase offer.
     */
    public val offerTags: List<String>?

    /**
     * Discount display info for the one-time purchase offer, null if base plan / no discount.
     */
    public val discountDisplayInfo: DiscountDisplayInfo?

    /**
     * Contains only data that is required to make the purchase.
     */
    public val purchasingData: PurchasingData

    /**
     * The context from which this one-time purchase offer was obtained.
     */
    public val presentedOfferingContext: PresentedOfferingContext?
}
