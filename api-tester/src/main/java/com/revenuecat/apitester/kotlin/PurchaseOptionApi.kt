package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.GooglePurchaseOption
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseOption

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class PurchaseOptionApi {

    fun checkPurchaseOption(purchaseOption: PurchaseOption) {
        val phases: List<PricingPhase> = purchaseOption.pricingPhases
        val tags: List<String> = purchaseOption.tags
        val isBasePlan: Boolean = purchaseOption.isBasePlan
    }

    fun checkGooglePurchaseOption(googlePurchaseOption: GooglePurchaseOption) {
        checkPurchaseOption(googlePurchaseOption)
        val token: String = googlePurchaseOption.token
    }
}
