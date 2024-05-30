package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams

/**
 * This object holds the information used when upgrading from another sku.
 * @property oldSku The oldSku to upgrade from.
 * @property replacementMode The [BillingFlowParams.SubscriptionUpdateParams.ReplacementMode] to use when upgrading the
 * given oldSku.
 */
@Deprecated(
    "Use .oldProductId() and .googleReplacementMode() in PurchaseParams.Builder instead",
    ReplaceWith("PurchaseParams.Builder.oldProductId() and PurchaseParams.Builder.googleReplacementMode()"),
)
data class UpgradeInfo(
    val oldSku: String,
    @BillingFlowParams.SubscriptionUpdateParams.ReplacementMode val replacementMode: Int? = null,
)
