package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener

internal data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean? = null,
    val updatedPurchaserInfoListener: UpdatedPurchaserInfoListener? = null,
    val purchaseCallbacks: Map<String, MakePurchaseListener> = emptyMap(),
    val productChangeCallbacks: Map<String, ProductChangeListener> = emptyMap(),
    val lastSentPurchaserInfo: PurchaserInfo? = null,
    val productChangeSku: String? = null
)
