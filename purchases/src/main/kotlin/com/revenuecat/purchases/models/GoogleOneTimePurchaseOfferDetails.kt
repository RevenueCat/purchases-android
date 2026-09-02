package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext
import dev.drewhamilton.poko.Poko

/**
 * Defines Google-specific offer details for a one-time (INAPP) product.
 */
@Poko
public class GoogleOneTimePurchaseOfferDetails @JvmOverloads constructor(
    /**
     * Product ID of the one-time product.
     */
    override val productId: String,

    /**
     * Price information for this offer.
     */
    override val price: Price,

    /**
     * Offer ID set in Play Console, or null if none.
     */
    override val offerId: String?,

    /**
     * The offer token used to purchase this offer.
     */
    override val offerToken: String?,

    /**
     * List of tags associated with this offer.
     */
    override val offerTags: List<String>?,

    /**
     * Discount display info for this offer, or null if base plan/no discount.
     */
    override val discountDisplayInfo: ProductDetails.OneTimePurchaseOfferDetails.DiscountDisplayInfo? = null,

    /**
     * The `ProductDetails` object from BillingClient this offer was created from.
     */
    public val productDetails: ProductDetails,

    /**
     * The context from which this offer details was obtained.
     */
    override val presentedOfferingContext: PresentedOfferingContext? = null,
) : OneTimePurchaseOfferDetails {

    internal constructor(
        offerDetails: GoogleOneTimePurchaseOfferDetails,
        presentedOfferingContext: PresentedOfferingContext?,
    ) : this(
        offerDetails.productId,
        offerDetails.price,
        offerDetails.offerId,
        offerDetails.offerToken,
        offerDetails.offerTags,
        offerDetails.discountDisplayInfo,
        offerDetails.productDetails,
        presentedOfferingContext,
    )

    override val purchasingData: PurchasingData
        get() = GooglePurchasingData.InAppProduct(
            productId = productId,
            productDetails = productDetails,
            selectedOfferToken = offerToken,
        )
}
