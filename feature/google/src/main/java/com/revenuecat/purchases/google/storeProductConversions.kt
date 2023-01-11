package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct

// In-apps don't have base plan nor offers
fun ProductDetails.toInAppStoreProduct(): StoreProduct = this.toStoreProduct(emptyList(), null)

fun ProductDetails.toStoreProduct(
    offerDetails: List<ProductDetails.SubscriptionOfferDetails>,
    defaultOffer: ProductDetails.SubscriptionOfferDetails?
) =
    GoogleStoreProduct(
        productId,
        productType.toRevenueCatProductType(),
        createOneTimeProductPrice(),
        title,
        description,
        offerDetails.firstOrNull { it.isBasePlan }?.subscriptionBillingPeriod,
        offerDetails.map { it.toSubscriptionOption() },
        defaultOffer?.toSubscriptionOption(),
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

fun List<ProductDetails>.toStoreProducts(): List<StoreProduct> {
    val storeProducts = mutableListOf<StoreProduct>()
    forEach { productDetails ->
        val basePlans = productDetails.subscriptionOfferDetails?.filter { it.isBasePlan } ?: emptyList()

        val offerDetailsBySubPeriod = productDetails.subscriptionOfferDetails?.groupBy {
            it.subscriptionBillingPeriod
        } ?: emptyMap()

        // Maps basePlans to StoreProducts, if any
        // Otherwise, maps productDetail to StoreProduct
        basePlans.takeUnless { it.isEmpty() }?.forEach { basePlan ->
            val basePlanBillingPeriod = basePlan.subscriptionBillingPeriod
            val offerDetailsForBasePlan = offerDetailsBySubPeriod[basePlanBillingPeriod] ?: emptyList()
            val defaultOfferDetails = basePlan // TODOBC5: Change logic here for default offer.
            productDetails.toStoreProduct(offerDetailsForBasePlan, defaultOfferDetails).let {
                storeProducts.add(it)
            }
        } ?: productDetails.toInAppStoreProduct().let { storeProducts.add(it) }
    }
    return storeProducts
}
