package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams

/**
 * This object holds the information used when upgrading from another sku.
 * @property oldProductId The old product ID to upgrade from.
 * @property prorationMode The [BillingFlowParams.ProrationMode] to use when upgrading the given oldSku. Defaults to
 * [BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION].
 */
data class UpgradeInfo @JvmOverloads constructor(
    val oldProductId: String,
    @BillingFlowParams.ProrationMode val prorationMode: Int =
        BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION
)
