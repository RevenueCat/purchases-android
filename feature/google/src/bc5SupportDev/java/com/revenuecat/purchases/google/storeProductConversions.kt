package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BC5StoreProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreProductImpl
import org.json.JSONObject

fun SkuDetails.toStoreProduct() =
    StoreProductImpl(
        sku,
        type.toRevenueCatProductType(),
        price,
        priceAmountMicros,
        priceCurrencyCode,
        originalPrice,
        originalPriceAmountMicros,
        title,
        description,
        subscriptionPeriod.takeIf { it.isNotBlank() },
        freeTrialPeriod.takeIf { it.isNotBlank() },
        introductoryPrice.takeIf { it.isNotBlank() },
        introductoryPriceAmountMicros,
        introductoryPricePeriod.takeIf { it.isNotBlank() },
        introductoryPriceCycles,
        iconUrl,
        JSONObject(originalJson)
    )

fun ProductDetails.PricingPhases.readable(): String {
    return "[" + pricingPhaseList.map { (if (it.billingCycleCount > 1) (it.billingCycleCount.toString() + "*") else "") + it.billingPeriod + "@" + it.formattedPrice }
        .joinToString() + "]"
}

fun ProductDetails.toStoreProduct(offerToken: String, pricingPhases: ProductDetails.PricingPhases, offerTags: List<String>) =
    BC5StoreProduct(
        productId,
        ProductType.SUBS,
        pricingPhases.readable(),
        100,
        "USD",
        "originalPrice",
        100,
        title,
        offerTags.toString(),
        pricingPhases.pricingPhaseList.last().billingPeriod,
        null,
        null,
        0,
        null,
        0,
        "icon",
        JSONObject(mapOf("offerToken" to offerToken)),
        this,
        offerToken,
        pricingPhases
    )

fun ProductDetails.toStoreProduct(rcPackage: Package): StoreProduct? {
    val duration = rcPackage.duration ?: return null // TODO error here, shoudln't be trying to make productdetails if BC5 not enabled
    val offers = listOf()

    subscriptionOfferDetails?.forEach { offerDetails ->
        if (offerDetails.pricingPhases.pricingPhaseList.size == 1) {
            // this is a base plan
            if (offerDetails.pricingPhases.pricingPhaseList[0].billingPeriod == duration) {
                // this is the base plan for this package!

            } else {
                // do nothing, this is a base plan for a different package
            }
            return@forEach
        } else {
            // this is an offer
            // TODO create storeproduct for offer? or maybe this is a new object entirely, Offer object
            // offers.add(createdProduct)
        }
        val firstPhase = offerDetails.pricingPhases.pricingPhaseList[0]
        val price = firstPhase?.formattedPrice ?: ""
        val priceAmountMicros = firstPhase?.priceAmountMicros ?: 0L
        val priceCurrencyCode = firstPhase?.priceCurrencyCode ?: ""
        val period = firstPhase?.billingPeriod
        // TODO etc...trial is first phase, then intro price, etc
    }


//    return StoreProduct(
//        productId,
//        productType.toProductType(),
//        price,
//        priceAmountMicros,
//        priceCurrencyCode,
//        originalPrice,
//        originalPriceAmountMicros,
//        title,
//        description,
//        subscriptionPeriod.takeIf { it.isNotBlank() },
//        freeTrialPeriod.takeIf { it.isNotBlank() },
//        introductoryPrice.takeIf { it.isNotBlank() },
//        introductoryPriceAmountMicros,
//        introductoryPricePeriod.takeIf { it.isNotBlank() },
//        introductoryPriceCycles,
//        iconUrl,
//        JSONObject(originalJson)
//    )
}

