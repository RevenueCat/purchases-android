package com.revenuecat.purchases

import com.android.billingclient.api.SkuDetails

internal class ProductInfo(
    val productID: String,
    val offeringIdentifier: String? = null,
    val skuDetails: SkuDetails? = null
) {

    val price: Double? = skuDetails?.priceAmount
    val currency: String? = skuDetails?.priceCurrencyCode
    val duration: String? = skuDetails?.subscriptionPeriod?.takeUnless { it.isEmpty() }
    val introDuration: String? = skuDetails?.introductoryPricePeriod?.takeUnless { it.isEmpty() }
    val trialDuration: String? = skuDetails?.freeTrialPeriod?.takeUnless { it.isEmpty() }

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
        result = 31 * result + (skuDetails?.hashCode() ?: 0)
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
