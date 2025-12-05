package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleOneTimePurchaseOption

internal fun ProductDetails.OneTimePurchaseOfferDetails.toOneTimePurchaseOption(
    productId: String,
    productDetails: ProductDetails,
): GoogleOneTimePurchaseOption {

    return GoogleOneTimePurchaseOption(
        id = productId,
        tags = offerTags,
        presentedOfferingContext = null,
        productDetails = productDetails
    )
}