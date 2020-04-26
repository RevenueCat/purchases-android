package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams

/**
 * This object holds the information used when upgrading from another sku.
 * @property oldSku The oldSku to upgrade from.
 * @property prorationMode The [BillingFlowParams.ProrationMode] to use when upgrading the given oldSku.
 */
data class UpgradeInfo(
    val oldSku: String,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
