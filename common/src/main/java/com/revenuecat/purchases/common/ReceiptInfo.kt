package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.StoreProduct

class ReceiptInfo(
    val productIDs: List<String>,
    val offeringIdentifier: String? = null,
    val storeProduct: StoreProduct? = null,
    val price: Double? = storeProduct?.priceAmountMicros?.div(MICROS_MULTIPLIER.toDouble()),
    val currency: String? = storeProduct?.priceCurrencyCode
) {

    val duration: String? = storeProduct?.subscriptionPeriod?.takeUnless { it.isEmpty() }
    val introDuration: String? = storeProduct?.introductoryPricePeriod?.takeUnless { it.isEmpty() }
    val trialDuration: String? = storeProduct?.freeTrialPeriod?.takeUnless { it.isEmpty() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiptInfo

        if (productIDs != other.productIDs) return false
        if (offeringIdentifier != other.offeringIdentifier) return false
        if (price != other.price) return false
        if (currency != other.currency) return false
        if (duration != other.duration) return false
        if (introDuration != other.introDuration) return false
        if (trialDuration != other.trialDuration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = productIDs.hashCode()
        result = 31 * result + (offeringIdentifier?.hashCode() ?: 0)
        result = 31 * result + (storeProduct?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ReceiptInfo(" +
            "productIDs='${productIDs.joinToString()}', " +
            "offeringIdentifier=$offeringIdentifier, " +
            "price=$price, " +
            "currency=$currency, " +
            "duration=$duration, " +
            "introDuration=$introDuration, " +
            "trialDuration=$trialDuration)"
    }
}
