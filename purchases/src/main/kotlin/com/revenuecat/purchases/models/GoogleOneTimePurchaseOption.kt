package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext

class GoogleOneTimePurchaseOption(
    override val tags: List<String>?,

    /**
     * If this OneTimePurchaseOption represents a purchase option, this will be the purchase option ID.
     * If it represents an offer, it will be purchaseOptionId:offerId
     */
    val productId: String,

    /**
     * The id of the base plan that this `GoogleOneTimePurchaseOption` belongs to.
     */
    val purchaseOptionId: String,

    /**
     * If this represents an offer, the offerId set in the Play Console.
     * Null otherwise.
     */
    val offerId: String?,

    /**
     * The `ProductDetails` object this `GoogleOneTimePurchaseOption` was created from.
     * Use to get underlying BillingClient information.
     */
    val productDetails: ProductDetails,

    override val presentedOfferingContext: PresentedOfferingContext?
): OneTimePurchaseOption {
    override val purchasingData: PurchasingData
        get() = GooglePurchasingData.InAppProduct(
            productId = this.id,
            productDetails = productDetails,
        )

    override val id: String
        get() = purchaseOptionId + if (offerId.isNullOrBlank()) "" else ":$offerId"
}