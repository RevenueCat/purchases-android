package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.models.StoreProduct

internal data class PurchaseInformation(
    val title: String,
    val durationTitle: String,
    val price: String,
    val expirationDateString: String?,
    val willRenew: Boolean,
    val active: Boolean,
    val product: StoreProduct,
)
