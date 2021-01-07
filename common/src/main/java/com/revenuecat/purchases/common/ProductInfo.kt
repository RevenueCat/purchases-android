package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.ProductDetails

class ProductInfo(
    val productID: String,
    val offeringIdentifier: String? = null,
    val productDetails: ProductDetails? = null
) {

    val price: Double? = productDetails?.priceAmountMicros?.div(1000000.0)
    val currency: String? = productDetails?.priceCurrencyCode
    val duration: String? = productDetails?.subscriptionPeriod?.takeUnless { it.isEmpty() }
    val introDuration: String? = productDetails?.introductoryPricePeriod?.takeUnless { it.isEmpty() }
    val trialDuration: String? = productDetails?.freeTrialPeriod?.takeUnless { it.isEmpty() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductInfo

        if (productID != other.productID) return false
        if (offeringIdentifier != other.offeringIdentifier) return false
        if (price != other.price) return false
        if (currency != other.currency) return false
        if (duration != other.duration) return false
        if (introDuration != other.introDuration) return false
        if (trialDuration != other.trialDuration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = productID.hashCode()
        result = 31 * result + (offeringIdentifier?.hashCode() ?: 0)
        result = 31 * result + (productDetails?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ProductInfo(" +
            "productID='$productID', " +
            "offeringIdentifier=$offeringIdentifier, " +
            "price=$price, " +
            "currency=$currency, " +
            "duration=$duration, " +
            "introDuration=$introDuration, " +
            "trialDuration=$trialDuration)"
    }
}
