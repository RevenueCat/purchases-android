package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.NewPurchaseCallback

internal data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean? = null,
    val purchaseCallbacksByProductId: Map<String, NewPurchaseCallback> = emptyMap(),
    val productChangeCallback: NewPurchaseCallback? = null,
    val appInBackground: Boolean = true,
    val firstTimeInForeground: Boolean = true
)
