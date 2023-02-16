package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.OfferingFactory
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.PurchaseStrings

// In-apps don't have base plan nor offers
fun ProductDetails.toInAppStoreProduct(): StoreProduct? = this.toStoreProduct(emptyList())

fun ProductDetails.toStoreProduct(
    offerDetails: List<ProductDetails.SubscriptionOfferDetails>
): GoogleStoreProduct? {
    val subscriptionOptions = offerDetails.map { it.toSubscriptionOption(productId, this) }
    val defaultOffer = subscriptionOptions.findDefaultOffer()

    val basePlanPrice = subscriptionOptions.firstOrNull { it.isBasePlan }?.fullPricePhase?.price
    val price = createOneTimeProductPrice() ?: basePlanPrice ?: return null

    return GoogleStoreProduct(
        productId,
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

            productDetails.toStoreProduct(offerDetailsForBasePlan)?.let {
                storeProducts.add(it)
            } ?: log(
                LogIntent.RC_ERROR, PurchaseStrings.INVALID_PRODUCT_NO_PRICE.format(productDetails.productId)
            )
        } ?: productDetails.toInAppStoreProduct()?.let {
            storeProducts.add(it)
        } ?: log(
            LogIntent.RC_ERROR, PurchaseStrings.INVALID_PRODUCT_NO_PRICE.format(productDetails.productId)
        )
    }
    return storeProducts
}

class GoogleOfferingFactory : OfferingFactory() {
    override fun Map<String, List<StoreProduct>>.findMatchingProduct(
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? {
        return this.getMatchingGoogleProduct(productIdentifier, planIdentifier)
    }

    fun Map<String, List<StoreProduct>>.getMatchingGoogleProduct(
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? {
        if (planIdentifier == null) {
            // It could be an INAPP or a mis-configured subscription
            // Try to find INAPP, otherwise null
            return this[productIdentifier]
                .takeIf { it?.size == 1 }
                ?.takeIf { it[0].type == ProductType.INAPP }
                ?.get(0)
        }
        val storeProducts: List<StoreProduct>? = this[productIdentifier]
        return storeProducts?.firstOrNull { storeProduct ->
            storeProduct.subscriptionOptions.firstOrNull { it.isBasePlan }?.id == planIdentifier
        }
    }
}
