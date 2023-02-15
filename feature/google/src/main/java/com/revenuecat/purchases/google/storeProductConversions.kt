package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.PurchaseStrings

// In-apps don't have base plan nor offers
fun ProductDetails.toInAppStoreProduct(): GoogleStoreProduct? = this.toStoreProduct(emptyList())

fun ProductDetails.toStoreProduct(
    offerDetails: List<ProductDetails.SubscriptionOfferDetails>
): GoogleStoreProduct? {
    val subscriptionOptions = offerDetails.map { it.toSubscriptionOption(productId, this) }
    val defaultOffer = subscriptionOptions.findDefaultOffer()

    val basePlan = subscriptionOptions.firstOrNull { it.isBasePlan }
    val basePlanPrice = basePlan?.fullPricePhase?.price
    val price = createOneTimeProductPrice() ?: basePlanPrice ?: return null

    return GoogleStoreProduct(
        productId,
        basePlan?.id,
        productType.toRevenueCatProductType(),
        price,
        title,
        description,
        offerDetails.firstOrNull { it.isBasePlan }?.subscriptionBillingPeriod,
        subscriptionOptions,
        defaultOffer,
        this
    )
}

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

@SuppressWarnings("NestedBlockDepth")
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

            productDetails.toStoreProduct(offerDetailsForBasePlan).let {
                it?.let { product -> storeProducts.add(product) } ?: log(
                    LogIntent.RC_ERROR, PurchaseStrings.INVALID_PRODUCT_NO_PRICE
                        .format(productDetails.productId)
                )
            }
        } ?: productDetails.toInAppStoreProduct().let {
            it?.let { product -> storeProducts.add(product) } ?: log(
                LogIntent.RC_ERROR, PurchaseStrings.INVALID_PRODUCT_NO_PRICE
                    .format(productDetails.productId)
            )
        }
    }
    return storeProducts
}
