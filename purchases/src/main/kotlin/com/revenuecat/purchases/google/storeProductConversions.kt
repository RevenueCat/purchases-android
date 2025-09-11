package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.strings.PurchaseStrings

// In-apps don't have base plan nor offers
internal fun ProductDetails.toInAppStoreProduct(): StoreProduct? = this.toStoreProduct(emptyList())

internal fun ProductDetails.toStoreProduct(
    offerDetails: List<ProductDetails.SubscriptionOfferDetails>,
): GoogleStoreProduct? {
    val subscriptionOptions = if (productType.toRevenueCatProductType() == ProductType.SUBS) {
        SubscriptionOptions(
            offerDetails.map { it.toSubscriptionOption(productId, this) },
        )
    } else {
        null
    }

    val basePlan = subscriptionOptions?.basePlan
    val basePlanPrice = basePlan?.fullPricePhase?.price
    val price = createOneTimeProductPrice() ?: basePlanPrice ?: return null

    return GoogleStoreProduct(
        productId = productId,
        basePlanId = basePlan?.id,
        type = productType.toRevenueCatProductType(),
        price = price,
        name = name,
        title = title,
        description = description,
        period = basePlan?.billingPeriod,
        subscriptionOptions = subscriptionOptions,
        defaultOption = subscriptionOptions?.defaultOffer,
        productDetails = this,
        presentedOfferingContext = null,
    )
}

private fun ProductDetails.createOneTimeProductPrice(): Price? {
    return if (productType.toRevenueCatProductType() == ProductType.INAPP) {
        oneTimePurchaseOfferDetails?.let {
            Price(
                it.formattedPrice,
                it.priceAmountMicros,
                it.priceCurrencyCode,
            )
        }
    } else {
        null
    }
}

@SuppressWarnings("NestedBlockDepth")
internal fun List<ProductDetails>.toStoreProducts(): List<StoreProduct> {
    val storeProducts = mutableListOf<StoreProduct>()
    forEach { productDetails ->
        val basePlans = productDetails.subscriptionOfferDetails?.filter { it.isBasePlan } ?: emptyList()

        val offerDetailsByBasePlanId = productDetails.subscriptionOfferDetails?.groupBy {
            it.basePlanId
        } ?: emptyMap()

        // Maps basePlans to StoreProducts, if any
        // Otherwise, maps productDetail to StoreProduct
        basePlans.takeUnless { it.isEmpty() }?.forEach { basePlan ->
            val offerDetailsForBasePlan = offerDetailsByBasePlanId[basePlan.basePlanId] ?: emptyList()

            productDetails.toStoreProduct(offerDetailsForBasePlan)?.let {
                storeProducts.add(it)
            } ?: log(LogIntent.RC_ERROR) {
                PurchaseStrings.INVALID_PRODUCT_NO_PRICE.format(productDetails.productId)
            }
        } ?: productDetails.toInAppStoreProduct()?.let {
            storeProducts.add(it)
        } ?: log(LogIntent.RC_ERROR) {
            PurchaseStrings.INVALID_PRODUCT_NO_PRICE.format(productDetails.productId)
        }
    }
    return storeProducts
}

// TODO: Write tests for this
// TODO: This should throw a PurchasesError instead of IllegalArgumentException
val List<StoreProduct>.purchasingData: GooglePurchasingData.SubscriptionBundle
    get() {
        require(this.all { it.type == ProductType.SUBS }) {
            "All items in subscription bundle must be subscriptions."
        }

        val googleSubscriptions = this
            .filter({ it.googleProduct != null })
            .filter({ it.purchasingData is GooglePurchasingData.Subscription })

        require(googleSubscriptions.size >= 2) {
            "Subscription bundle must contain at least two subscriptions."
        }

        require(googleSubscriptions.size <= 50) {
            "Subscription bundle cannot contain more than 50 subscriptions. Found: ${googleSubscriptions.size}"
        }

        val billingPeriods = googleSubscriptions.mapNotNull { it.period }.distinct()
        require(billingPeriods.size <= 1) {
            "All items in a subscription bundle must have the same recurring billing period."
        }

        val bundledSubscriptions: List<GooglePurchasingData.Subscription> = googleSubscriptions
            .mapNotNull { it.purchasingData }
            .filterIsInstance<GooglePurchasingData.Subscription>()

        val productId = bundledSubscriptions
            .mapNotNull({ it.productId})
            .joinToString("-")

        return GooglePurchasingData.SubscriptionBundle(
            productId = productId,
            bundledSubscriptions = bundledSubscriptions
        )
    }
