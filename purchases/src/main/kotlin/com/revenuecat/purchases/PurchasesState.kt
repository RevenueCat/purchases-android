package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener

internal data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean? = null,
    val updatedCustomerInfoListener: UpdatedCustomerInfoListener? = null,
    val purchaseCallbacks: Map<String, PurchaseCallback> = emptyMap(),
    val productChangeCallback: ProductChangeCallback? = null,
    val lastSentCustomerInfo: CustomerInfo? = null,
    val appInBackground: Boolean = true,
    val firstTimeInForeground: Boolean = true
)
