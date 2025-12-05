package com.revenuecat.purchases.models

import com.revenuecat.purchases.PresentedOfferingContext

/**
 * A purchase-able entity for a one-time purchase product.
 */
interface OneTimePurchaseOption {
    /**
     * TODO: Update the terminology on this
     * For Google one-time purchases:
     * If this OneTimePurchaseOption represents a base plan, this will be the basePlanId.
     * If it represents an offer, it will be {basePlanId}:{offerId}
     *
     * Not applicable for Amazon subscriptions.
     */
    val id: String


    /**
     * TODO: Update the terminology on this
     *
     * Tags defined on the base plan or offer. Keep in mind that offers automatically
     * inherit their base plan's tag. Empty for Amazon.
     */
    val tags: List<String>?

    /**
     * The context from which this one-time purchase option was obtained.
     *
     * Null if not using RevenueCat offerings system, if fetched directly via `Purchases.getProducts`,
     * or on restores/syncs.
     */
    val presentedOfferingContext: PresentedOfferingContext?

    val purchasingData: PurchasingData
}