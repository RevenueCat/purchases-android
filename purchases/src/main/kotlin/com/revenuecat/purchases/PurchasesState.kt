package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener

internal data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean? = null,
    val updatedPurchaserInfoListener: UpdatedPurchaserInfoListener? = null,
    val purchaseCallbacks: Map<String, PurchaseCallback> = emptyMap(),
    val productChangeCallback: ProductChangeCallback? = null,
    val lastSentPurchaserInfo: PurchaserInfo? = null,
    val appInBackground: Boolean = true,
    val firstTimeInForeground: Boolean = true
)
