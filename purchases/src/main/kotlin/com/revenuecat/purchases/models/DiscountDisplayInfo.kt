package com.revenuecat.purchases.models

/**
 * Represents discount display information for a one-time purchase offer.
 */
public interface DiscountDisplayInfo {
    /**
     * Percentage discount offered by this one-time purchase offer, or null if unavailable.
     */
    public val percentageDiscount: Int?

    /**
     * Discount amount details for this one-time purchase offer, or null if unavailable.
     */
    public val discountAmount: Price?
}

