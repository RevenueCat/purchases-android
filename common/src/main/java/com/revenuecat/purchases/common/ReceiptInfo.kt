package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct

data class ReceiptInfo(
    val productIDs: List<String>,
    val offeringIdentifier: String? = null,
    val subscriptionOptionId: String? = null,
    val storeProduct: StoreProduct? = null,

    val price: Double? = storeProduct?.oneTimeProductPrice?.priceAmountMicros?.div(MICROS_MULTIPLIER.toDouble()),
    val currency: String? = storeProduct?.oneTimeProductPrice?.currencyCode
) {

    val duration: String? = storeProduct?.subscriptionPeriod?.takeUnless { it.isEmpty() }
    val pricingPhases: List<PricingPhase>? =
        storeProduct?.subscriptionOptions?.firstOrNull { it.id == subscriptionOptionId }?.pricingPhases
}
