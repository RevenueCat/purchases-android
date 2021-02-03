package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.models.PurchaseDetails

data class ReplaceSkuInfo(
    val oldPurchase: PurchaseDetails,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
