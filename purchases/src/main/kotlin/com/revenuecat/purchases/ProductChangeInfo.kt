package com.revenuecat.purchases

import com.revenuecat.purchases.models.GoogleProrationMode

/**
 * This object holds the information used when changing from another product.
 * @property oldProductId The old product ID to upgrade from.
 * @property googleProrationMode The [GoogleProrationMode] to use when upgrading the given oldProductId. Defaults to
 * [GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION].
 */
data class ProductChangeInfo @JvmOverloads constructor(
    val oldProductId: String,
    val googleProrationMode: GoogleProrationMode = GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION
)
