package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.createOneTimeProductPrice
import com.revenuecat.purchases.models.GoogleOneTimePurchaseOption
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.OneTimePurchaseOption
import com.revenuecat.purchases.models.OneTimePurchaseOptions
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.strings.PurchaseStrings

// In-apps don't have base plan nor offers
internal fun ProductDetails.toInAppStoreProduct(
    oneTimePurchaseOfferDetails: List<ProductDetails.OneTimePurchaseOfferDetails>
): StoreProduct? = this.toStoreProduct(
    subscriptionOfferDetails = emptyList(),
    oneTimePurchaseOfferDetails = oneTimePurchaseOfferDetails,
)

internal fun ProductDetails.toStoreProduct(
    subscriptionOfferDetails: List<ProductDetails.SubscriptionOfferDetails>,
    oneTimePurchaseOfferDetails: List<ProductDetails.OneTimePurchaseOfferDetails>
): GoogleStoreProduct? {
    val productType = productType.toRevenueCatProductType()

    return when(productType) {
        ProductType.SUBS -> createStoreProductForSubscription(
            offerDetails = subscriptionOfferDetails
        )
        ProductType.INAPP -> createStoreProductForOneTimePurchase(
            offerDetails = oneTimePurchaseOfferDetails
        )
        ProductType.UNKNOWN -> null
    }
}

private fun ProductDetails.createStoreProductForOneTimePurchase(
    offerDetails: List<ProductDetails.OneTimePurchaseOfferDetails>
): GoogleStoreProduct? {
    val price = createOneTimeProductPrice() ?: return null
    val otpOptions = OneTimePurchaseOptions(
        oneTimePurchaseOptions = offerDetails.map {
            it.offerTags
            it.offerToken
            it.toOneTimePurchaseOption(
                productId = productId,
                productDetails = this
            ) as OneTimePurchaseOption
        }
    )

    return GoogleStoreProduct(
        productId = productId,
        basePlanId = null,
        type = productType.toRevenueCatProductType(),
        price = price,
        name = name,
        title = title,
        description = description,
        period = null,
        subscriptionOptions = null,
        defaultOption = null,
        productDetails = this,
        presentedOfferingContext = null,
        oneTimePurchaseOptions = OneTimePurchaseOptions(oneTimePurchaseOptions = otpOptions)
    )
}

private fun ProductDetails.createStoreProductForSubscription(
    offerDetails: List<ProductDetails.SubscriptionOfferDetails>,
): GoogleStoreProduct? {
    val subscriptionOptions = SubscriptionOptions(
        offerDetails.map { it.toSubscriptionOption(productId, this) },
    )

    val basePlan = subscriptionOptions.basePlan
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
        defaultOption = subscriptionOptions.defaultOffer,
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

            productDetails.toStoreProduct(
                subscriptionOfferDetails = offerDetailsForBasePlan,
                oneTimePurchaseOfferDetails = emptyList()
            )?.let {
                storeProducts.add(it)
            } ?: log(LogIntent.RC_ERROR) {
                PurchaseStrings.INVALID_PRODUCT_NO_PRICE.format(productDetails.productId)
            }
        } ?: productDetails.toInAppStoreProduct(
            oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetailsList ?: emptyList()
        )?.let {
            storeProducts.add(it)
        } ?: log(LogIntent.RC_ERROR) {
            PurchaseStrings.INVALID_PRODUCT_NO_PRICE.format(productDetails.productId)
        }
    }
    return storeProducts
}
