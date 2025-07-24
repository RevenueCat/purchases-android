@file:JvmSynthetic

package com.revenuecat.purchases.teststore

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.networking.RCBillingProductResponse
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct

internal object TestStoreProductConverter {

    @JvmSynthetic
    @Suppress("LongMethod")
    @Throws(PurchasesException::class)
    fun convertToStoreProduct(productResponse: RCBillingProductResponse): StoreProduct {
        val defaultPurchaseOptionId = productResponse.defaultPurchaseOptionId
        val purchaseOptions = productResponse.purchaseOptions

        val purchaseOptionKey = defaultPurchaseOptionId ?: purchaseOptions.keys.first()
        val purchaseOption = purchaseOptions[purchaseOptionKey]!!

        val basePrice: Price?
        var period: Period? = null
        var freeTrialPeriod: Period? = null
        var introPrice: Price? = null

        if (purchaseOption.basePrice != null) {
            val basePriceObj = purchaseOption.basePrice
            basePrice = Price(
                formatted = formatPrice(basePriceObj.amountMicros, basePriceObj.currency),
                amountMicros = basePriceObj.amountMicros,
                currencyCode = basePriceObj.currency,
            )
        } else {
            val basePhase = purchaseOption.base
            if (basePhase?.price != null) {
                val priceObj = basePhase.price
                basePrice = Price(
                    formatted = formatPrice(priceObj.amountMicros, priceObj.currency),
                    amountMicros = priceObj.amountMicros,
                    currencyCode = priceObj.currency,
                )
                if (basePhase.periodDuration != null) {
                    period = Period.create(basePhase.periodDuration)
                }
            } else {
                throw PurchasesException(
                    PurchasesError(
                        PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                        "Base priceis required for test subscription products",
                    ),
                )
            }

            val trialPhase = purchaseOption.trial
            if (trialPhase?.periodDuration != null) {
                freeTrialPeriod = Period.create(trialPhase.periodDuration)
            }

            val introPhase = purchaseOption.introPrice
            if (introPhase?.price != null) {
                val priceObj = introPhase.price
                introPrice = Price(
                    formatted = formatPrice(priceObj.amountMicros, priceObj.currency),
                    amountMicros = priceObj.amountMicros,
                    currencyCode = priceObj.currency,
                )
            }
        }

        return TestStoreProduct(
            id = productResponse.identifier,
            name = productResponse.title,
            title = productResponse.title,
            description = productResponse.description ?: "",
            price = basePrice,
            period = period,
            freeTrialPeriod = freeTrialPeriod,
            introPrice = introPrice,
        )
    }

    @Suppress("MagicNumber")
    private fun formatPrice(amountMicros: Long, currencyCode: String): String {
        return "$currencyCode ${"%.2f".format(amountMicros / 1_000_000.0)}"
    }
}
