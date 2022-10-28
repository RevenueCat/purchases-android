package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct

// In-apps don't have base plan nor offers
fun ProductDetails.toStoreProduct(): StoreProduct = this.toStoreProduct(null, emptyList())

fun ProductDetails.toStoreProduct(
    basePlan: ProductDetails.SubscriptionOfferDetails?,
    offers: List<ProductDetails.SubscriptionOfferDetails>
) =
    GoogleStoreProduct(
        productId,
        productType.toRevenueCatProductType(),
        createOneTimeProductPrice(),
        title,
        description,
        basePlan?.pricingPhases?.pricingPhaseList?.get(0)?.billingPeriod,
        offers.map { it.toPurchaseOption() },
        this
    )

private fun ProductDetails.createOneTimeProductPrice(): Price? {
    return if (productType.toRevenueCatProductType() == ProductType.INAPP) {
        oneTimePurchaseOfferDetails?.let {
            Price(
                it.formattedPrice,
                it.priceAmountMicros,
                it.priceCurrencyCode
            )
        }
    } else null
}
