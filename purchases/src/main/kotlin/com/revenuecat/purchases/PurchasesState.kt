package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ProductChangeCallback

internal data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean? = null,
    val purchaseCallbacksByProductId: Map<String, ProductChangeCallback> = emptyMap(),
    val productChangeCallback: ProductChangeCallback? = null,
    val appInBackground: Boolean = true,
    val firstTimeInForeground: Boolean = true
)
