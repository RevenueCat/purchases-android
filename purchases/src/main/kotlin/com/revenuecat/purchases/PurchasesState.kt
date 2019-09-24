package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import java.util.Date

data class PurchasesState(
    val allowSharingPlayStoreAccount: Boolean = false,
    val finishTransactions: Boolean = true,
    val appUserID: String = "",
    val updatedPurchaserInfoListener: UpdatedPurchaserInfoListener? = null,
    val cachedOfferings: Offerings? = null,
    val purchaseCallbacks: Map<String, MakePurchaseListener> = emptyMap(),
    val lastSentPurchaserInfo: PurchaserInfo? = null,
    val cachesLastUpdated: Date = Date(0)
)