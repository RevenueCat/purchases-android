package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.GoogleOneTimePurchaseOfferDetails
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.OneTimePurchaseOfferDetailsList
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOptions
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

    val oneTimePurchaseOfferDetailsList =
        if (productType.toRevenueCatProductType() == ProductType.INAPP) {
            OneTimePurchaseOfferDetailsList(
                this.oneTimePurchaseOfferDetailsList?.map {
                    it.toGoogleOneTimePurchaseOfferDetails(productId, this)
                } ?: listOfNotNull(
                    this.oneTimePurchaseOfferDetails?.toGoogleOneTimePurchaseOfferDetails(productId, this)
                )
            )
        } else {
            null
        }

    val basePlanSubscription = subscriptionOptions?.basePlan
    val basePlanOneTimeOffer = oneTimePurchaseOfferDetailsList?.basePlan

    val basePlanPrice = basePlanSubscription?.fullPricePhase?.price
        ?: basePlanOneTimeOffer?.price
        ?: createOneTimeProductPrice()
        ?: return null


    return GoogleStoreProduct(
        productId = productId,
        basePlanId = basePlanSubscription?.id,
        type = productType.toRevenueCatProductType(),
        price = basePlanPrice,
        name = name,
        title = title,
        description = description,
        period = basePlanSubscription?.billingPeriod,
        subscriptionOptions = subscriptionOptions,
        defaultOption = subscriptionOptions?.defaultOffer,
        productDetails = this,
        presentedOfferingContext = null,
        oneTimePurchaseOfferDetailsList = oneTimePurchaseOfferDetailsList,
        defaultOneTimeOffer = oneTimePurchaseOfferDetailsList?.defaultOffer
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

internal fun ProductDetails.OneTimePurchaseOfferDetails.toGoogleOneTimePurchaseOfferDetails(
    productId: String,
    productDetails: ProductDetails,
): GoogleOneTimePurchaseOfferDetails {
    val price = Price(
        formatted = this.formattedPrice,
        amountMicros = this.priceAmountMicros,
        currencyCode = this.priceCurrencyCode,
    )
    return GoogleOneTimePurchaseOfferDetails(
        productId = productId,
        price = price,
        offerId = this.offerId,
        offerToken = this.offerToken,
        offerTags = this.offerTags,
        discountDisplayInfo = this.discountDisplayInfo,
        productDetails = productDetails,
        presentedOfferingContext = null,
    )
}

@OptIn(InternalRevenueCatAPI::class)
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
