package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams

internal data class ReplaceSkuInfo(
    val oldPurchase: PurchaseHistoryRecordWrapper,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
