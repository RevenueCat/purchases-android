package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.GooglePurchaseOption
import com.revenuecat.purchases.models.PurchaseOption

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock", "RemoveExplicitTypeArguments", "RedundantLambdaArrow")
private class PurchaseOptionApi {

    fun checkPurchaseOption(purchaseOption: PurchaseOption) {
        val phases = purchaseOption.pricingPhases
        val tags = purchaseOption.tags
        val isBasePlan = purchaseOption.isBasePlan
    }

    fun checkGooglePurchaseOption(googlePurchaseOption: GooglePurchaseOption) {
        checkPurchaseOption(googlePurchaseOption)
        val token = googlePurchaseOption.token
    }

}
