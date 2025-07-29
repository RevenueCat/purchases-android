@file:JvmSynthetic

package com.revenuecat.purchases.teststore

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.networking.WebBillingProductResponse
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.TestStoreProduct

internal object TestStoreProductConverter {

    @JvmSynthetic
    @Suppress("LongMethod")
    @Throws(PurchasesException::class)
    fun convertToStoreProduct(productResponse: WebBillingProductResponse): TestStoreProduct {
        val defaultPurchaseOptionId = productResponse.defaultPurchaseOptionId
        val purchaseOptions = productResponse.purchaseOptions

        val purchaseOptionKey = defaultPurchaseOptionId ?: purchaseOptions.keys.first()
        val purchaseOption = purchaseOptions[purchaseOptionKey] ?: run {
            throw PurchasesException(
                PurchasesError(
                    PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                    "No purchase option found for product ${productResponse.identifier}",
                ),
            )
        }

        val basePrice: Price?
        var period: Period? = null
        var freeTrialPricingPhase: PricingPhase? = null
        var introPricePricingPhase: PricingPhase? = null

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
                        "Base price is required for test subscription products",
                    ),
                )
            }

            val trialPhase = purchaseOption.trial
            if (trialPhase?.periodDuration != null && trialPhase.cycleCount != null) {
                freeTrialPricingPhase = PricingPhase(
                    billingPeriod = Period.create(trialPhase.periodDuration),
                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                    billingCycleCount = trialPhase.cycleCount,
                    price = Price(
                        formatted = "Free",
                        amountMicros = 0L,
                        currencyCode = basePrice.currencyCode,
                    ),
                )
            }

            val introPhase = purchaseOption.introPrice
            if (introPhase?.price != null && introPhase.periodDuration != null && introPhase.cycleCount != null) {
                val priceObj = introPhase.price
                introPricePricingPhase = PricingPhase(
                    billingPeriod = Period.create(introPhase.periodDuration),
                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                    billingCycleCount = introPhase.cycleCount,
                    price = Price(
                        formatted = formatPrice(priceObj.amountMicros, priceObj.currency),
                        amountMicros = priceObj.amountMicros,
                        currencyCode = priceObj.currency,
                    ),
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
            freeTrialPricingPhase = freeTrialPricingPhase,
            introPricePricingPhase = introPricePricingPhase,
        )
    }

    @Suppress("MagicNumber")
    private fun formatPrice(amountMicros: Long, currencyCode: String): String {
        return "$currencyCode ${"%.2f".format(amountMicros / 1_000_000.0)}"
    }
}
