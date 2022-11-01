package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.BillingFeature

@Suppress("unused")
private class PurchasesBC5API {
    fun check(billingFeature: BillingFeature) {
        when (billingFeature) {
            BillingFeature.PRICE_CHANGE_CONFIRMATION,
            BillingFeature.SUBSCRIPTIONS,
            BillingFeature.SUBSCRIPTIONS_UPDATE -> {
            }
        }.exhaustive
    }
}
