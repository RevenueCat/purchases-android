package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback

internal data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean? = null,
    val purchaseCallbacksByProductId: Map<String, PurchaseCallback> = emptyMap(),
    val deprecatedProductChangeCallback: ProductChangeCallback? = null,
    val appInBackground: Boolean = true,
    val firstTimeInForeground: Boolean = true,
)
