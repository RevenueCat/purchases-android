package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct

class ReceiptInfo(
    val productIDs: List<String>,
    val offeringIdentifier: String? = null,
    val storeProduct: StoreProduct? = null, // this appears unused. if we remove, we have to pass full pricing phases
    val purchaseOptionId: String? = null,

    // TODO BC5 - replace price/currency with making a single PricingPhase for amazon observermode products?
    val price: Double? = 1.0, // only passed for amazon observer mode
    val currency: String? = "USD" // only passed for amazon observer mode
) {

    val duration: String? = storeProduct?.subscriptionPeriod?.takeUnless { it.isEmpty() }
    val pricingPhases: List<PricingPhase>? =
        storeProduct?.purchaseOptions?.first { it.id == purchaseOptionId }?.pricingPhases

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiptInfo

        if (productIDs != other.productIDs) return false
        if (offeringIdentifier != other.offeringIdentifier) return false
        if (price != other.price) return false
        if (currency != other.currency) return false
        if (duration != other.duration) return false
        if (purchaseOptionId != other.purchaseOptionId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = productIDs.hashCode()
        result = 31 * result + (offeringIdentifier?.hashCode() ?: 0)
        result = 31 * result + (storeProduct?.hashCode() ?: 0)
        result = 31 * result + (purchaseOptionId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ReceiptInfo(" +
            "productIDs='${productIDs.joinToString()}', " +
            "offeringIdentifier=$offeringIdentifier, " +
            "storeProduct=${storeProduct.toString()}, " +
            "purchaseOptionId=${purchaseOptionId}, " +
            "pricingPhases=${pricingPhases.toString()}, " +
            "price=$price, " +
            "currency=$currency, " +
            "duration=$duration)"
    }
}
