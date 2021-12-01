package com.revenuecat.purchases.google

import com.revenuecat.purchases.models.PurchaseState as RevenuecatPurchaseState
import com.android.billingclient.api.Purchase.PurchaseState as GooglePurchaseState

fun Int.toRevenueCatPurchaseState(): RevenuecatPurchaseState {
    return when (this) {
        GooglePurchaseState.UNSPECIFIED_STATE -> RevenuecatPurchaseState.UNSPECIFIED_STATE
        GooglePurchaseState.PURCHASED -> RevenuecatPurchaseState.PURCHASED
        GooglePurchaseState.PENDING -> RevenuecatPurchaseState.PENDING
        else -> RevenuecatPurchaseState.UNSPECIFIED_STATE
    }
}

fun RevenuecatPurchaseState.toGooglePurchaseState(): Int {
    return when (this) {
        RevenuecatPurchaseState.UNSPECIFIED_STATE -> GooglePurchaseState.UNSPECIFIED_STATE
        RevenuecatPurchaseState.PURCHASED -> GooglePurchaseState.PURCHASED
        RevenuecatPurchaseState.PENDING -> GooglePurchaseState.PENDING
    }
}
