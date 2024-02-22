package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError

interface SyncAttributesAndOfferingsCallback {
    fun onSuccess(offerings: Offerings)
    fun onError(error: PurchasesError)
}
