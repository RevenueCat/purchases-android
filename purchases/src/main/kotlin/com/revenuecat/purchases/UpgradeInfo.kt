package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams

data class UpgradeInfo(
    val oldSku: String,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)