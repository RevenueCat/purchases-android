package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError

interface SyncPurchasesCallback {
    fun onSuccess()
    fun onError(error: PurchasesError)
}
