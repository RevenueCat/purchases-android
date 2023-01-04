package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams

/**
 * This object holds the information used when upgrading from another sku.
 * @property oldProductId The old product ID to upgrade from.
 * @property prorationMode The [BillingFlowParams.ProrationMode] to use when upgrading the given oldSku.
 */
data class UpgradeInfo @JvmOverloads constructor(
    val oldProductId: String,
    @Deprecated(
        "Replaced with oldProductId",
        ReplaceWith("oldProductId")
    )
    val oldSku: String = oldProductId,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
