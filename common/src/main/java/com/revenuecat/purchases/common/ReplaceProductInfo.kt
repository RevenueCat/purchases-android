package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.models.StoreTransaction

data class ReplaceProductInfo(
    val oldPurchase: StoreTransaction,
    @BillingFlowParams.ProrationMode val prorationMode: Int
)
