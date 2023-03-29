package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.PlatformProductId
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption

class ReceiptInfo(
    val productIDs: List<String>,
    val offeringIdentifier: String? = null,
    val subscriptionOptionId: String? = null,
    val storeProduct: StoreProduct? = null,

    val price: Double? = storeProduct?.price?.amountMicros?.div(MICROS_MULTIPLIER.toDouble()),
    val currency: String? = storeProduct?.price?.currencyCode
) {

    val duration: String? = storeProduct?.period?.iso8601?.takeUnless { it.isEmpty() }

    private val subscriptionOption: SubscriptionOption? =
        storeProduct?.subscriptionOptions?.firstOrNull { it.id == subscriptionOptionId }
    val pricingPhases: List<PricingPhase>? =
        subscriptionOption?.pricingPhases

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiptInfo

        if (productIDs != other.productIDs) return false
        if (offeringIdentifier != other.offeringIdentifier) return false
        if (storeProduct != other.storeProduct) return false
        if (price != other.price) return false
        if (currency != other.currency) return false
        if (subscriptionOptionId != other.subscriptionOptionId) return false

        return true
    }

    internal val platformProductIds: List<PlatformProductId>?
        get() {
            val storeProductPlatformProductId =
                subscriptionOption?.platformProductId ?: storeProduct?.platformProductId

            // ReceiptInfo can be created with only productIDs but it might also be created with
            // a StoreProduct
            // We want the PlatformProductID with most info (like GooglePlatformProductId from a SubscriptionOption)
            // so this logic prevents duplicate productIds (PlatformProductID) from being returned
            val platformProductIds = productIDs
                .filter { it != storeProductPlatformProductId?.productId }
                .map { PlatformProductId(it) }

            return platformProductIds + listOfNotNull(storeProductPlatformProductId)
        }

    override fun hashCode(): Int {
        var result = productIDs.hashCode()
        result = 31 * result + (offeringIdentifier?.hashCode() ?: 0)
        result = 31 * result + (storeProduct?.hashCode() ?: 0)
        result = 31 * result + (subscriptionOptionId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ReceiptInfo(" +
            "productIDs='${productIDs.joinToString()}', " +
            "offeringIdentifier=$offeringIdentifier, " +
            "storeProduct=$storeProduct, " +
            "subscriptionOptionId=$subscriptionOptionId, " +
            "pricingPhases=$pricingPhases, " +
            "price=$price, " +
            "currency=$currency, " +
            "duration=$duration)"
    }
}
