package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.RecurrenceMode
import org.json.JSONObject

private fun ProductDetails.toStoreProduct() =
    GoogleStoreProduct(
        productId,
        productType.toRevenueCatProductType(),
        createPrice(),
        title,
        description,
        "", // TODO pass from package
        createPurchaseOptions(),
        this
    )

private fun ProductDetails.createPrice(): Price? {
    return if (productType.toRevenueCatProductType() == ProductType.INAPP)
        oneTimePurchaseOfferDetails?.let {
            Price(
                it.formattedPrice,
                it.priceAmountMicros,
                it.priceCurrencyCode
            )
        }
    else null
}

private fun ProductDetails.createPurchaseOptions(): List<PurchaseOption> {
    // TODO copy code converting productdetails to pricingphases from poc branch
    return listOf()
}