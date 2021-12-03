package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.models.PaymentTransaction

data class ReplaceSkuInfo(
    val oldPurchase: PaymentTransaction,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
