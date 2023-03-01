package com.revenuecat.purchases

import com.revenuecat.purchases.models.GoogleProrationMode

/**
 * This object holds the information used when upgrading from another sku.
 * @property oldProductId The old product ID to upgrade from.
 * @property googleProrationMode The [GoogleProrationMode] to use when upgrading the given oldProductId. Defaults to
 * [GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION].
 */
// TODO BC5 deprecate
data class UpgradeInfo @JvmOverloads constructor(
    val oldProductId: String,
    val googleProrationMode: GoogleProrationMode = GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION
)
