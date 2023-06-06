package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.BillingFeature

@Suppress("unused")
private class PurchasesBC4API {
    fun check(billingFeature: BillingFeature) {
        when (billingFeature) {
            BillingFeature.PRICE_CHANGE_CONFIRMATION,
            BillingFeature.SUBSCRIPTIONS,
            BillingFeature.SUBSCRIPTIONS_UPDATE,
            BillingFeature.IN_APP_ITEMS_ON_VR,
            BillingFeature.SUBSCRIPTIONS_ON_VR,
            -> {}
        }.exhaustive
    }
}
