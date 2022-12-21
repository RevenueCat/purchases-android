package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.models.StoreTransaction

data class ReplaceSkuInfo(
    val oldPurchase: StoreTransaction, // TODO should we just pass the token?
    // TODO if we only apply one, why do we let them choose?
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
