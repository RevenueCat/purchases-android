package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.BillingFeature

@Suppress("unused", "UNUSED_VARIABLE")
private class BillingFeatureAPI {
    fun check(billingFeature: BillingFeature) {
        when (billingFeature) {
            BillingFeature.PRICE_CHANGE_CONFIRMATION,
            BillingFeature.SUBSCRIPTIONS,
            BillingFeature.SUBSCRIPTIONS_UPDATE,
            -> {
            }
        }.exhaustive
    }
}
