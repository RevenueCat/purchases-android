package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingFlowParams

data class ReplaceSkuInfo(
    val oldPurchase: PurchaseHistoryRecordWrapper,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
