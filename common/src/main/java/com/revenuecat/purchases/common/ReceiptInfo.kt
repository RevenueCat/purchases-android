package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct

class ReceiptInfo(
    val productIDs: List<String>,
    val offeringIdentifier: String? = null,
    val purchaseOptionId: String? = null,
    val storeProduct: StoreProduct? = null,

    val price: Double? = storeProduct?.oneTimeProductPrice?.priceAmountMicros?.div(MICROS_MULTIPLIER.toDouble()),
    val currency: String? = storeProduct?.oneTimeProductPrice?.currencyCode
) {

    val duration: String? = storeProduct?.subscriptionPeriod?.takeUnless { it.isEmpty() }
    val pricingPhases: List<PricingPhase>? =
        storeProduct?.purchaseOptions?.firstOrNull { it.id == purchaseOptionId }?.pricingPhases

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiptInfo

        if (productIDs != other.productIDs) return false
        if (offeringIdentifier != other.offeringIdentifier) return false
        if (price != other.price) return false
        if (currency != other.currency) return false
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
            "storeProduct=$storeProduct, " +
            "purchaseOptionId=$purchaseOptionId, " +
            "pricingPhases=$pricingPhases, " +
            "price=$price, " +
            "currency=$currency, " +
            "duration=$duration)"
    }
}
