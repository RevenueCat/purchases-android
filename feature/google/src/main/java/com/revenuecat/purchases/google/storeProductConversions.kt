package com.revenuecat.purchases.google

import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct

// In-apps don't have base plan nor offers
fun ProductDetails.toInAppStoreProduct(): StoreProduct = this.toStoreProduct(emptyList(), null)

fun ProductDetails.toStoreProduct(
    offerDetails: List<ProductDetails.SubscriptionOfferDetails>,
    defaultOffer: ProductDetails.SubscriptionOfferDetails?
): GoogleStoreProduct {
    val subscriptionOptions = offerDetails.map { it.toSubscriptionOption(productId, this) }

    val bestOffer = findBestOffer(subscriptionOptions)

    return GoogleStoreProduct(
        productId,
        productType.toRevenueCatProductType(),
        createOneTimeProductPrice(),
        title,
        description,
        offerDetails.firstOrNull { it.isBasePlan }?.subscriptionBillingPeriod,
        offerDetails.map {
            it.toSubscriptionOption(
                productId,
                this
            )
        },
        defaultOffer?.toSubscriptionOption(
            productId,
            this
        ),
        this
    )
}

private fun findBestOffer(subscriptionOptions: List<GoogleSubscriptionOption>): GoogleSubscriptionOption? {
    Log.d("JOSH", "STARTING BEST OPTION")

    val validOffers = subscriptionOptions
        .filter { !it.isBasePlan }
        .filter { !it.tags.contains("rc-ignore-best-offer") }


    // Option 1 - Find longest free pricing phase
    val offerWithLongestFreePricingPhase = validOffers.mapNotNull { offer ->
        // Finds longest free pricing phase for an offer
        offer.pricingPhases.filter { pricingPhase ->
            pricingPhase.priceAmountMicros == 0L
        }.map {
            Pair(offer, parseBillPeriodToDays(it.billingPeriod))
        }.maxByOrNull { it.second }
    }.maxByOrNull { it.second }?.first

    if (offerWithLongestFreePricingPhase != null) {
        return offerWithLongestFreePricingPhase
    }

    // Option 2 - Find cheapest pricing phase

    return subscriptionOptions.firstOrNull { it.isBasePlan }
}

// Would use Duration.parse but only available API 26 and up
private fun parseBillPeriodToDays(period: String): Int {
    val regex = "^P(?!\$)(\\d+(?:\\.\\d+)?Y)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?W)?(\\d+(?:\\.\\d+)?D)?(T(?=\\d)(\\d+(?:\\.\\d+)?H)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?S)?)?\$"
        .toRegex()
        .matchEntire(period)

    regex?.let { periodResult ->
        val toInt = fun(part: String): Int {
            Log.d("JOSH", "PART=$part")
            return part.dropLast(1).toIntOrNull() ?: 0
        }

        val (year, month, week, day) = periodResult.destructured
        return (toInt(year) * 365) + (toInt(month) * 28) + (toInt(week) * 7) + toInt(day)
    }

    return 0
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
